package voice.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import voice.core.data.Book
import voice.core.data.Chapter
import voice.core.data.ChapterMark
import voice.core.data.markForPosition
import voice.core.data.repo.BookRepository
import voice.core.playback.session.EXTRA_MARK_START_MS
import voice.core.playback.session.MediaId
import voice.core.playback.session.MediaItemProvider
import voice.core.playback.session.toMediaIdOrNull

/**
 * ForwardingPlayer placed between [VoicePlayer] and the [androidx.media3.session.MediaLibrarySession].
 *
 * Translates the position/duration that the session (and therefore the Android media notification
 * seek bar plus any [androidx.media3.session.MediaController]) sees so that they reflect the
 * current [ChapterMark] rather than the whole audio file. It also keeps the current
 * [MediaItem]'s metadata title in sync with the active mark so the notification shows the mark
 * name when present, falling back to the chapter name and finally to the book name.
 *
 * The actual audio playback is unchanged: the wrapped [VoicePlayer] (and the underlying ExoPlayer)
 * keep operating on file-relative positions.
 */
@Inject
class MarkAwarePlayer(
  private val voicePlayer: VoicePlayer,
  private val mediaItemProvider: MediaItemProvider,
  private val repo: BookRepository,
  private val scope: CoroutineScope,
) : ForwardingPlayer(voicePlayer) {

  private var currentBook: Book? = null
  private var markTickJob: Job? = null
  private var bookObserverJob: Job? = null

  init {
    voicePlayer.addListener(
      object : Player.Listener {
        override fun onMediaItemTransition(
          mediaItem: MediaItem?,
          reason: Int,
        ) {
          syncCurrentBook(mediaItem)
        }

        override fun onPositionDiscontinuity(
          oldPosition: Player.PositionInfo,
          newPosition: Player.PositionInfo,
          reason: Int,
        ) {
          maybeReplaceMediaItemForMark()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
          if (isPlaying) startMarkTicking() else stopMarkTicking()
        }
      },
    )

    // Apply translation immediately when the wrapper is created in case the underlying
    // player is already prepared/playing (e.g., service was recreated mid-playback).
    syncCurrentBook(voicePlayer.currentMediaItem)
    if (voicePlayer.isPlaying) startMarkTicking()
  }

  private fun syncCurrentBook(mediaItem: MediaItem?) {
    val bookId = (mediaItem?.mediaId?.toMediaIdOrNull() as? MediaId.Chapter)?.bookId
    if (bookId == null) {
      currentBook = null
      bookObserverJob?.cancel()
      bookObserverJob = null
      return
    }
    if (currentBook?.id == bookId) {
      maybeReplaceMediaItemForMark()
      return
    }
    bookObserverJob?.cancel()
    bookObserverJob = scope.launch {
      repo.flow(bookId)
        .filterNotNull()
        .distinctUntilChangedBy { it.content.name to it.content.cover }
        .collect { book ->
          currentBook = book
          // Force the current MediaItem to be rebuilt so renames / cover changes propagate
          // to the notification, even when the active mark hasn't moved.
          maybeReplaceMediaItemForMark(force = true)
        }
    }
  }

  private fun currentChapter(): Chapter? {
    val book = currentBook ?: return null
    val mediaId = voicePlayer.currentMediaItem?.mediaId?.toMediaIdOrNull() as? MediaId.Chapter
      ?: return null
    return book.chapters.firstOrNull { it.id == mediaId.chapterId }
  }

  private fun currentMark(): ChapterMark? {
    val chapter = currentChapter() ?: return null
    val rawPos = voicePlayer.currentPosition.takeUnless { it == C.TIME_UNSET } ?: 0L
    return chapter.markForPosition(rawPos)
  }

  private fun maybeReplaceMediaItemForMark(force: Boolean = false) {
    val book = currentBook ?: return
    val chapter = currentChapter() ?: return
    val mark = currentMark() ?: return
    val index = voicePlayer.currentMediaItemIndex
    if (index < 0 || index >= voicePlayer.mediaItemCount) return
    if (!force) {
      val current = voicePlayer.getMediaItemAt(index)
      val currentMarkStart = current.mediaMetadata.extras
        ?.getLong(EXTRA_MARK_START_MS, Long.MIN_VALUE)
        ?: Long.MIN_VALUE
      if (currentMarkStart == mark.startMs) return
    }
    voicePlayer.replaceMediaItem(index, mediaItemProvider.mediaItemForMark(chapter, mark, book.content))
  }

  private fun startMarkTicking() {
    if (markTickJob?.isActive == true) return
    markTickJob = scope.launch {
      while (isActive) {
        delay(250)
        maybeReplaceMediaItemForMark()
      }
    }
  }

  private fun stopMarkTicking() {
    markTickJob?.cancel()
    markTickJob = null
  }

  /**
   * Cancel any long-running observers/tickers started by this wrapper. Safe to call multiple
   * times. Mainly useful when the owning scope is not torn down (e.g. in tests).
   */
  fun releaseObservers() {
    bookObserverJob?.cancel()
    bookObserverJob = null
    stopMarkTicking()
  }

  override fun getDuration(): Long {
    val mark = currentMark() ?: return super.getDuration()
    return mark.endMs - mark.startMs + 1
  }

  override fun getContentDuration(): Long = duration

  override fun getCurrentPosition(): Long {
    val pos = super.getCurrentPosition()
    val mark = currentMark() ?: return pos
    if (pos == C.TIME_UNSET) return pos
    val markDuration = mark.endMs - mark.startMs + 1
    return (pos - mark.startMs).coerceIn(0L, markDuration)
  }

  override fun getContentPosition(): Long = currentPosition

  override fun getBufferedPosition(): Long {
    val buf = super.getBufferedPosition()
    val mark = currentMark() ?: return buf
    if (buf == C.TIME_UNSET) return buf
    val markDuration = mark.endMs - mark.startMs + 1
    return (buf - mark.startMs).coerceIn(0L, markDuration)
  }

  override fun seekTo(positionMs: Long) {
    val mark = currentMark()
    if (mark == null) {
      super.seekTo(positionMs)
      return
    }

    val maxOffset = (mark.endMs - mark.startMs).coerceAtLeast(0L)
    val clamped = positionMs.coerceIn(0L, maxOffset)
    super.seekTo(mark.startMs + clamped)
  }
}
