package voice.core.playback

import android.content.ComponentName
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.zacsweers.metro.Inject
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
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.data.store.SeekTimeStore
import voice.core.logging.api.Logger
import voice.core.playback.history.ListeningHistoryRecorder
import voice.core.playback.history.PlaybackPosition
import voice.core.playback.history.playbackPosition
import voice.core.playback.misc.Decibel
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

@Inject
class PlayerController(
  private val context: Context,
  @CurrentBookStore
  private val currentBookStoreId: DataStore<BookId?>,
  private val bookRepository: BookRepository,
  private val mediaItemProvider: MediaItemProvider,
  private val historyRecorder: ListeningHistoryRecorder,
  @SeekTimeStore
  private val seekTimeStore: DataStore<Int>,
) {

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

  /**
   * @param type how the jump is recorded in the listening history: [ListeningEvent.Type.Seek],
   * [ListeningEvent.Type.ChapterChange], [ListeningEvent.Type.BookmarkJump] or [ListeningEvent.Type.JumpBack].
   */
  fun setPosition(
    time: Long,
    id: ChapterId,
    type: ListeningEvent.Type = ListeningEvent.Type.Seek,
  ) = executeAfterPrepare { controller ->
    val bookId = currentBookStoreId.data.first() ?: return@executeAfterPrepare
    val book = bookRepository.get(bookId) ?: return@executeAfterPrepare
    val playbackItem = book.playbackItemForPosition(
      chapterId = id,
      positionInChapterMs = time,
    )
    if (playbackItem != null) {
      controller.record(type, to = PlaybackPosition(bookId, id, time))
      controller.seekTo(playbackItem.index, playbackItem.positionInMediaItem(time))
    }
  }

  fun pauseIfCurrentBookDifferentFrom(id: BookId) {
    scope.launch {
      val controller = awaitConnect() ?: return@launch
      val currentBookId = controller.currentBookId()
      if (currentBookId != null && currentBookId != id) {
        if (controller.playWhenReady) {
          controller.record(ListeningEvent.Type.Pause)
        }
        controller.pause()
      }
    }
  }

  fun skipSilence(skip: Boolean) = executeAfterPrepare { controller ->
    controller.record(ListeningEvent.Type.SkipSilenceChanged, value = skip.toString())
    controller.sendCustomCommand(CustomCommand.SetSkipSilence(skip))
  }

  fun fastForward(source: ListeningEvent.Source = ListeningEvent.Source.App) = executeAfterPrepare { controller ->
    controller.record(ListeningEvent.Type.SkipForward, source, value = seekTimeStore.data.first().toString())
    controller.seekForward()
  }

  fun rewind(source: ListeningEvent.Source = ListeningEvent.Source.App) = executeAfterPrepare { controller ->
    controller.record(ListeningEvent.Type.SkipBack, source, value = seekTimeStore.data.first().toString())
    controller.seekBack()
  }

  fun previous() = executeAfterPrepare { controller ->
    val currentIndex = controller.currentMediaItemIndex
    val index = if (controller.currentPosition > THRESHOLD_FOR_BACK_SEEK_MS) {
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

  fun next() = executeAfterPrepare { controller ->
    val index = controller.nextMediaItemIndex
    if (index != C.INDEX_UNSET) {
      controller.record(ListeningEvent.Type.ChapterChange, to = controller.playbackPosition(index, 0))
    }
    controller.sendCustomCommand(CustomCommand.ForceSeekToNext)
  }

  /**
   * @param source recorded in the listening history, or null to not record it.
   */
  fun play(source: ListeningEvent.Source? = ListeningEvent.Source.App) = executeAfterPrepare { controller ->
    if (source != null && !controller.playWhenReady) {
      controller.record(ListeningEvent.Type.Play, source)
    }
    controller.play()
  }

  fun playPause(source: ListeningEvent.Source = ListeningEvent.Source.App) = executeAfterPrepare { controller ->
    if (controller.isPlaying) {
      controller.record(ListeningEvent.Type.Pause, source)
      controller.pause()
    } else {
      controller.record(ListeningEvent.Type.Play, source)
      controller.play()
    }
  }

  /**
   * Records an event at the current position in the listening history.
   */
  fun record(
    type: ListeningEvent.Type,
    source: ListeningEvent.Source,
    value: String? = null,
  ) {
    scope.launch {
      awaitConnect()?.record(type, source, value = value)
    }
  }

  private suspend fun MediaController.record(
    type: ListeningEvent.Type,
    source: ListeningEvent.Source = ListeningEvent.Source.App,
    to: PlaybackPosition? = null,
    value: String? = null,
  ) {
    val position = playbackPosition() ?: storedPosition() ?: return
    historyRecorder.record(
      type = type,
      source = source,
      position = position,
      to = to,
      value = value,
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

  fun pauseWithRewind(rewind: Duration) = executeAfterPrepare { controller ->
    // Seeking first lets the auto rewind of the pause go back from there, instead of racing with it.
    controller.seekBackBy(
      rewind = rewind,
      crossMediaItems = false,
    )
    controller.pause()
  }

  private fun MediaController.seekBackBy(
    rewind: Duration,
    crossMediaItems: Boolean,
  ) {
    var currentPosition = currentPosition.takeUnless { it == C.TIME_UNSET }
      ?.milliseconds
      ?: return
    var remaining = rewind
    var mediaItemIndex = currentMediaItemIndex.takeUnless { it == C.INDEX_UNSET } ?: return

    while (remaining > currentPosition) {
      if (!crossMediaItems) {
        seekTo(mediaItemIndex, 0)
        return
      }
      remaining -= currentPosition
      val previousMediaItemIndex = mediaItemIndex - 1
      if (previousMediaItemIndex < 0) {
        seekTo(0)
        return
      }
      currentPosition = getMediaItemAt(previousMediaItemIndex).mediaMetadata.durationMs?.milliseconds ?: return
      mediaItemIndex = previousMediaItemIndex
    }

    seekTo(mediaItemIndex, (currentPosition - remaining).inWholeMilliseconds)
  }

  fun setSpeed(speed: Float) = executeAfterPrepare { controller ->
    controller.record(ListeningEvent.Type.SpeedChanged, value = speed.toString())
    controller.setPlaybackSpeed(speed)
  }

  fun setGain(gain: Decibel) = executeAfterPrepare { controller ->
    controller.record(ListeningEvent.Type.VolumeBoostChanged, value = gain.value.toString())
    controller.sendCustomCommand(CustomCommand.SetGain(gain))
  }

  fun setVolume(volume: Float) = executeAfterPrepare {
    require(volume in 0F..1F)
    it.volume = volume
  }

  suspend fun livePlaybackState(bookId: BookId? = null): LivePlaybackState? {
    val controller = awaitConnect() ?: return null
    return controller.livePlaybackStateSnapshot(bookId)
  }

  fun livePlaybackStateFlow(bookId: BookId? = null): Flow<LivePlaybackState?> = callbackFlow {
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
  suspend fun awaitConnect(): MediaController? {
    return try {
      controller.await()
    } catch (e: Exception) {
      if (e is CancellationException) currentCoroutineContext().ensureActive()
      Logger.w(e, "Error while connecting to media controller")
      null
    }
  }
}

private const val THRESHOLD_FOR_BACK_SEEK_MS = 2000

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
