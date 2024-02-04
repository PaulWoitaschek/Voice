package voice.playback.player

import androidx.datastore.core.DataStore
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import de.paulwoitaschek.flowpref.Pref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import voice.common.BookId
import voice.common.pref.CurrentBook
import voice.common.pref.PrefKeys
import voice.data.BookContent
import voice.data.Chapter
import voice.data.repo.BookRepository
import voice.data.repo.ChapterRepo
import voice.logging.core.Logger
import voice.playback.misc.Decibel
import voice.playback.misc.VolumeGain
import voice.playback.session.MediaId
import voice.playback.session.MediaItemProvider
import voice.playback.session.toMediaIdOrNull
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class VoicePlayer
@Inject constructor(
  private val player: Player,
  private val repo: BookRepository,
  @CurrentBook
  private val currentBookId: DataStore<BookId?>,
  @Named(PrefKeys.SEEK_TIME)
  private val seekTimePref: Pref<Int>,
  @Named(PrefKeys.AUTO_REWIND_AMOUNT)
  private val autoRewindAmountPref: Pref<Int>,
  private val mediaItemProvider: MediaItemProvider,
  private val scope: CoroutineScope,
  private val chapterRepo: ChapterRepo,
  private val volumeGain: VolumeGain,
) : ForwardingPlayer(player) {

  fun forceSeekToNext() {
    scope.launch {
      val currentMediaItem = player.currentMediaItem ?: return@launch
      val marks = currentMediaItem.chapter()?.chapterMarks ?: return@launch
      val currentMarkIndex = marks.indexOfFirst { mark ->
        player.currentPosition in mark.startMs..mark.endMs
      }
      val nextMark = marks.getOrNull(currentMarkIndex + 1)
      if (nextMark != null) {
        player.seekTo(nextMark.startMs)
      } else {
        player.seekToNext()
      }
    }
  }

  private suspend fun MediaItem.chapter(): Chapter? {
    val mediaId = mediaId.toMediaIdOrNull() ?: return null
    if (mediaId !is MediaId.Chapter) return null
    return chapterRepo.get(mediaId.chapterId)
  }

  fun forceSeekToPrevious() {
    scope.launch {
      val currentMediaItem = player.currentMediaItem ?: return@launch
      val marks = currentMediaItem.chapter()?.chapterMarks ?: return@launch
      val currentPosition = player.currentPosition
      val currentMark = marks.firstOrNull { mark ->
        currentPosition in mark.startMs..mark.endMs
      } ?: marks.last()

      if (currentPosition - currentMark.startMs > THRESHOLD_FOR_BACK_SEEK_MS) {
        player.seekTo(currentMark.startMs)
      } else {
        val currentMarkIndex = marks.indexOf(currentMark)
        val previousMark = marks.getOrNull(currentMarkIndex - 1)
        if (previousMark != null) {
          player.seekTo(previousMark.startMs)
        } else {
          val currentMediaItemIndex = player.currentMediaItemIndex
          if (currentMediaItemIndex > 0) {
            val previousMediaItemIndex = currentMediaItemIndex - 1
            val previousMediaItemMarks = player.getMediaItemAt(previousMediaItemIndex).chapter()?.chapterMarks
              ?: return@launch
            player.seekTo(previousMediaItemIndex, previousMediaItemMarks.last().startMs)
          } else {
            player.seekTo(0)
          }
        }
      }
    }
  }

  override fun getAvailableCommands(): Player.Commands {
    /**
     * On Android 13, the notification always shows the "skip to next" and "skip to previous"
     * actions.
     * However these are also used internally when seeking for example through a bluetooth headset
     * We use these and delegate them to fast forward / rewind.
     * The player however only advertises the seek to next and previous item in the case
     * that it's not the first or last track. Therefore we manually advertise that these
     * are available.
     */
    return super.getAvailableCommands()
      .buildUpon()
      .addAll(
        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_PREVIOUS,
        Player.COMMAND_SEEK_TO_NEXT,
        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
      )
      .build()
  }

  override fun seekToPreviousMediaItem() {
    seekBack()
  }

  override fun seekToNextMediaItem() {
    seekForward()
  }

  override fun seekToPrevious() {
    seekBack()
  }

  override fun seekToNext() {
    seekForward()
  }

  override fun seekBack() {
    scope.launch {
      val skipAmount = seekTimePref.value.seconds

      val currentPosition = player.currentPosition.takeUnless { it == C.TIME_UNSET }
        ?.milliseconds
        ?.coerceAtLeast(ZERO)
        ?: return@launch

      val newPosition = currentPosition - skipAmount
      if (newPosition < ZERO) {
        val previousMediaItemIndex = previousMediaItemIndex.takeUnless { it == C.INDEX_UNSET }
        if (previousMediaItemIndex == null) {
          player.seekTo(0)
        } else {
          val previousMediaItem = player.getMediaItemAt(previousMediaItemIndex)
          val chapter = previousMediaItem.chapter() ?: return@launch
          val previousMediaItemDuration = chapter.duration.milliseconds
          player.seekTo(previousMediaItemIndex, (previousMediaItemDuration - newPosition.absoluteValue).inWholeMilliseconds)
        }
      } else {
        player.seekTo(newPosition.inWholeMilliseconds)
      }
    }
  }

  override fun seekForward() {
    val skipAmount = seekTimePref.value.seconds

    val currentPosition = player.currentPosition.takeUnless { it == C.TIME_UNSET }
      ?.milliseconds
      ?.coerceAtLeast(ZERO)
      ?: return
    val newPosition = currentPosition + skipAmount

    val duration = player.duration.takeUnless { it == C.TIME_UNSET }
      ?.milliseconds
      ?: return

    if (newPosition > duration) {
      val nextMediaItemIndex = nextMediaItemIndex.takeUnless { it == C.INDEX_UNSET }
        ?: return
      player.seekTo(nextMediaItemIndex, (duration - newPosition).absoluteValue.inWholeMilliseconds)
    } else {
      player.seekTo(newPosition.inWholeMilliseconds)
    }
  }

  override fun play() {
    playWhenReady = true
  }

  override fun setPlayWhenReady(playWhenReady: Boolean) {
    Logger.d("setPlayWhenReady=$playWhenReady")
    if (playWhenReady) {
      updateLastPlayedAt()
    } else {
      val currentPosition = player.currentPosition.takeUnless { it == C.TIME_UNSET }?.milliseconds ?: ZERO
      if (currentPosition > ZERO) {
        seekTo(
          (currentPosition - autoRewindAmountPref.value.seconds)
            .coerceAtLeast(ZERO)
            .inWholeMilliseconds,
        )
      }
    }
    super.setPlayWhenReady(playWhenReady)
  }

  override fun pause() {
    playWhenReady = false
  }

  private fun updateLastPlayedAt() {
    scope.launch {
      currentBookId.data.first()?.let { bookId ->
        repo.updateBook(bookId) {
          val lastPlayedAt = Instant.now()
          Logger.v("Update ${it.name}: lastPlayedAt to $lastPlayedAt")
          it.copy(lastPlayedAt = lastPlayedAt)
        }
      }
    }
  }

  override fun getPlaybackState(): Int = when (val state = super.getPlaybackState()) {
    // redirect buffering to ready to prevent visual artifacts on seeking
    Player.STATE_BUFFERING -> Player.STATE_READY
    else -> state
  }

  override fun setMediaItem(
    mediaItem: MediaItem,
    startPositionMs: Long,
  ) {
    setBook(mediaItem)
  }

  override fun setMediaItem(
    mediaItem: MediaItem,
    resetPosition: Boolean,
  ) {
    setBook(mediaItem)
  }

  override fun setMediaItems(mediaItems: List<MediaItem>) {
    val first = mediaItems.firstOrNull() ?: return
    setBook(first)
  }

  override fun setMediaItems(
    mediaItems: List<MediaItem>,
    resetPosition: Boolean,
  ) {
    val first = mediaItems.firstOrNull() ?: return
    setBook(first)
  }

  override fun setMediaItem(mediaItem: MediaItem) {
    setBook(mediaItem)
  }

  override fun setMediaItems(
    mediaItems: List<MediaItem>,
    startIndex: Int,
    startPositionMs: Long,
  ) {
    val first = mediaItems.firstOrNull() ?: return
    setBook(first)
  }

  private fun setBook(mediaItem: MediaItem) {
    Logger.v("setBook(${mediaItem.mediaId})")
    val mediaId = mediaItem.mediaId.toMediaIdOrNull()
    if (mediaId != null) {
      if (mediaId is MediaId.Book) {
        val book = runBlocking {
          repo.get(mediaId.id)
        }
        if (book != null) {
          player.setPlaybackSpeed(book.content.playbackSpeed)
          setSkipSilenceEnabled(book.content.skipSilence)
          volumeGain.gain = Decibel(book.content.gain)
          player.setMediaItems(
            mediaItemProvider.chapters(book),
            book.content.currentChapterIndex,
            book.content.positionInChapter,
          )
        }
      } else {
        Logger.w("Unexpected mediaId=$mediaId")
      }
    }
  }

  override fun setPlaybackSpeed(speed: Float) {
    super.setPlaybackSpeed(speed)
    scope.launch {
      updateBook { it.copy(playbackSpeed = speed) }
    }
  }

  fun setSkipSilenceEnabled(enabled: Boolean): Boolean {
    scope.launch {
      updateBook { it.copy(skipSilence = enabled) }
    }
    return if (player is ExoPlayer) {
      player.skipSilenceEnabled = enabled
      true
    } else {
      false
    }
  }

  fun setGain(gain: Decibel) {
    volumeGain.gain = gain
    scope.launch {
      updateBook { it.copy(gain = gain.value) }
    }
  }

  private suspend fun updateBook(update: (BookContent) -> BookContent) {
    val bookId = currentBookId.data.first() ?: return
    repo.updateBook(bookId, update)
  }

  suspend fun prepareCurrentBook() {
    val bookId = currentBookId.data.first() ?: return
    val book = repo.get(bookId) ?: return
    val item = mediaItemProvider.mediaItem(book)
    setMediaItem(item)
    prepare()
  }
}

private const val THRESHOLD_FOR_BACK_SEEK_MS = 2000
