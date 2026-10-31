package voice.core.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.asDeferred
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.logging.api.Logger
import voice.core.playback.history.ListeningHistoryRecorder
import voice.core.playback.history.PlaybackPosition
import voice.core.playback.history.playbackPosition
import voice.core.playback.misc.Decibel
import voice.core.playback.player.seekBackBy
import voice.core.playback.session.CustomCommand
import voice.core.playback.session.MediaItemProvider
import voice.core.playback.session.PlaybackService
import voice.core.playback.session.bookId
import voice.core.playback.session.playbackItemForPosition
import voice.core.playback.session.positionInMediaItem
import voice.core.playback.session.sendCustomCommand
import voice.core.playback.session.toMediaIdOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class PlayerControllerImpl(
  private val context: Context,
  private val currentBookStoreId: CurrentBookStore,
  private val bookRepository: BookRepository,
  private val mediaItemProvider: MediaItemProvider,
  private val historyRecorder: ListeningHistoryRecorder,
) : PlayerController {

  private var _controller: Deferred<MediaController> = newControllerAsync()

  private fun newControllerAsync() = MediaController
    .Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
    .buildAsync()
    .asDeferred()

  private val controller: Deferred<MediaController>
    get() {
      if (_controller.isCompleted) {
        val completedController = _controller.getCompleted()
        if (!completedController.isConnected) {
          completedController.release()
          _controller = newControllerAsync()
        }
      }
      return _controller
    }
  private val scope = CoroutineScope(Dispatchers.Main.immediate)

  override fun setPosition(
    time: Long,
    id: ChapterId,
    type: ListeningEvent.Type,
    source: ListeningEvent.Source,
  ) = executeAfterPrepare { controller ->
    val bookId = currentBookStoreId.data.first() ?: return@executeAfterPrepare
    val book = bookRepository.get(bookId) ?: return@executeAfterPrepare
    val playbackItem = book.playbackItemForPosition(
      chapterId = id,
      positionInChapterMs = time,
    )
    if (playbackItem != null) {
      controller.record(type, source, to = PlaybackPosition(bookId, id, time))
      controller.seekTo(playbackItem.index, playbackItem.positionInMediaItem(time))
    }
  }

  override fun pauseIfCurrentBookDifferentFrom(
    id: BookId,
    source: ListeningEvent.Source,
  ) {
    scope.launch {
      val controller = awaitConnect() ?: return@launch
      val currentBookId = controller.currentBookId()
      if (currentBookId != null && currentBookId != id) {
        if (controller.playWhenReady) {
          controller.record(ListeningEvent.Type.Pause, source)
        }
        controller.pause()
      }
    }
  }

  override fun pause() {
    scope.launch {
      awaitConnect()?.pause()
    }
  }

  override fun skipSilence(skip: Boolean) = executeAfterPrepare { controller ->
    controller.sendCustomCommand(CustomCommand.SetSkipSilence(skip))
  }

  override fun fastForward(source: ListeningEvent.Source) = executeAfterPrepare { controller ->
    controller.record(ListeningEvent.Type.SkipForward, source)
    controller.seekForward()
  }

  override fun rewind(source: ListeningEvent.Source) = executeAfterPrepare { controller ->
    controller.record(ListeningEvent.Type.SkipBack, source)
    controller.seekBack()
  }

  override fun previous() = executeAfterPrepare { controller ->
    val currentIndex = controller.currentMediaItemIndex
    val index = if (controller.currentPosition > controller.maxSeekToPreviousPosition) {
      currentIndex
    } else {
      controller.previousMediaItemIndex.takeUnless { it == C.INDEX_UNSET } ?: currentIndex
    }
    val to = controller.playbackPosition(index, 0)
    val type = previousJumpType(
      from = controller.playbackPosition(),
      to = to,
      sameItem = index == currentIndex,
    )
    if (type != null) {
      controller.record(type, to = to)
    }
    controller.sendCustomCommand(CustomCommand.ForceSeekToPrevious)
  }

  override fun next() = executeAfterPrepare { controller ->
    val index = controller.nextMediaItemIndex
    if (index != C.INDEX_UNSET) {
      controller.record(ListeningEvent.Type.ChapterChange, to = controller.playbackPosition(index, 0))
    }
    controller.sendCustomCommand(CustomCommand.ForceSeekToNext)
  }

  override fun play(source: ListeningEvent.Source?) = executeAfterPrepare { controller ->
    if (source != null && !controller.playWhenReady) {
      controller.record(ListeningEvent.Type.Play, source)
    }
    controller.play()
  }

  override fun playPause(source: ListeningEvent.Source) = executeAfterPrepare { controller ->
    if (!Util.shouldShowPlayButton(controller)) {
      controller.record(ListeningEvent.Type.Pause, source)
      controller.pause()
    } else {
      controller.record(ListeningEvent.Type.Play, source)
      controller.play()
    }
  }

  override fun record(
    type: ListeningEvent.Type,
    source: ListeningEvent.Source,
  ) {
    scope.launch {
      awaitConnect()?.record(type, source)
    }
  }

  private suspend fun MediaController.record(
    type: ListeningEvent.Type,
    source: ListeningEvent.Source = ListeningEvent.Source.App,
    to: PlaybackPosition? = null,
  ) {
    val position = playbackPosition() ?: storedPosition() ?: return
    historyRecorder.record(
      type = type,
      source = source,
      position = position,
      to = to,
    )
  }

  /**
   * Right after [maybePrepare] the player only holds the book until the service expands it into chapters,
   * so the position comes from the book itself.
   */
  private suspend fun storedPosition(): PlaybackPosition? {
    val bookId = currentBookStoreId.data.first() ?: return null
    val content = bookRepository.get(bookId)?.content ?: return null
    return PlaybackPosition(bookId, content.currentChapter, content.positionInChapter)
  }

  private suspend fun maybePrepare(controller: MediaController): Boolean {
    val bookId = currentBookStoreId.data.first() ?: return false
    if (controller.currentBookId() == bookId &&
      controller.playbackState in listOf(Player.STATE_READY, Player.STATE_BUFFERING)
    ) {
      return true
    }
    val book = bookRepository.get(bookId) ?: return false
    controller.setMediaItem(mediaItemProvider.mediaItem(book))
    controller.prepare()
    return true
  }

  private fun MediaController.currentBookId(): BookId? {
    val currentMediaItem = currentMediaItem ?: return null
    val mediaId = currentMediaItem.mediaId.toMediaIdOrNull() ?: return null
    return mediaId.bookId
  }

  override fun pauseWithRewind(rewind: Duration) = executeAfterPrepare { controller ->
    // Seeking first lets the auto rewind of the pause go back from there, instead of racing with it.
    controller.seekBackBy(
      rewind = rewind,
      crossMediaItems = true,
    )
    controller.pause()
  }

  override fun setSpeed(speed: Float) = executeAfterPrepare { controller ->
    controller.setPlaybackSpeed(speed)
  }

  override fun setGain(gain: Decibel) = executeAfterPrepare { controller ->
    controller.sendCustomCommand(CustomCommand.SetGain(gain))
  }

  override fun setVolume(volume: Float) = executeAfterPrepare {
    require(volume in 0F..1F)
    it.volume = volume
  }

  // the controller only answers on the main thread
  override suspend fun livePlaybackState(bookId: BookId?): LivePlaybackState? = withContext(Dispatchers.Main.immediate) {
    val controller = awaitConnect() ?: return@withContext null
    controller.livePlaybackStateSnapshot(bookId)
  }

  override fun livePlaybackStateFlow(bookId: BookId?): Flow<LivePlaybackState?> = callbackFlow {
    val controller = awaitConnect()
    if (controller == null) {
      trySend(null)
      close()
      return@callbackFlow
    }

    fun emitSnapshot() {
      trySend(controller.livePlaybackStateSnapshot(bookId))
    }

    var tickJob: Job? = null
    fun updateTicking() {
      if (!controller.isPlaying) {
        tickJob?.cancel()
        return
      }
      if (tickJob?.isActive == true) {
        return
      }
      tickJob = launch {
        while (isActive) {
          delay(250.milliseconds)
          emitSnapshot()
        }
      }
    }

    val listener = object : Player.Listener {
      override fun onEvents(
        player: Player,
        events: Player.Events,
      ) {
        if (events.containsAny(
            Player.EVENT_PLAY_WHEN_READY_CHANGED,
            Player.EVENT_MEDIA_ITEM_TRANSITION,
            Player.EVENT_PLAYBACK_STATE_CHANGED,
          )
        ) {
          emitSnapshot()
          updateTicking()
        }
        if (events.containsAny(
            Player.EVENT_POSITION_DISCONTINUITY,
            Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
          )
        ) {
          emitSnapshot()
        }
      }
    }

    controller.addListener(listener)
    emitSnapshot()
    updateTicking()
    awaitClose {
      tickJob?.cancel()
      controller.removeListener(listener)
    }
  }

  private inline fun executeAfterPrepare(crossinline action: suspend (MediaController) -> Unit) {
    scope.launch {
      val controller = awaitConnect() ?: return@launch
      if (maybePrepare(controller)) {
        action(controller)
      }
    }
  }

  @IgnorableReturnValue
  internal suspend fun awaitConnect(): MediaController? {
    return try {
      controller.await()
    } catch (e: Exception) {
      if (e is CancellationException) currentCoroutineContext().ensureActive()
      Logger.w(e, "Error while connecting to media controller")
      null
    }
  }
}

/**
 * Skipping back restarts the current chapter when it already played for a while, or when there is no chapter
 * before it. That is only a seek, so it can be undone only when it goes back far enough. Returns null when
 * playback is already where it would go, or when it's not known where it goes, as right after preparing, before
 * the book is split into its chapters.
 */
internal fun previousJumpType(
  from: PlaybackPosition?,
  to: PlaybackPosition?,
  sameItem: Boolean,
): ListeningEvent.Type? = when {
  to == null || to == from -> null
  sameItem -> ListeningEvent.Type.Seek
  else -> ListeningEvent.Type.ChapterChange
}
