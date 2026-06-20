package voice.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
  }

  private fun syncCurrentBook(mediaItem: MediaItem?) {
    val bookId = (mediaItem?.mediaId?.toMediaIdOrNull() as? MediaId.Chapter)?.bookId
    if (bookId == null) {
      currentBook = null
      return
    }
    if (currentBook?.id == bookId) {
      maybeReplaceMediaItemForMark()
      return
    }
    scope.launch {
      currentBook = repo.get(bookId)
      maybeReplaceMediaItemForMark()
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

  private fun maybeReplaceMediaItemForMark() {
    val book = currentBook ?: return
    val chapter = currentChapter() ?: return
    val mark = currentMark() ?: return
    val index = voicePlayer.currentMediaItemIndex
    if (index < 0 || index >= voicePlayer.mediaItemCount) return
    val current = voicePlayer.getMediaItemAt(index)
    val currentMarkStart = current.mediaMetadata.extras
      ?.getLong(EXTRA_MARK_START_MS, Long.MIN_VALUE)
      ?: Long.MIN_VALUE
    if (currentMarkStart == mark.startMs) return
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

  override fun getDuration(): Long {
    val mark = currentMark() ?: return super.getDuration()
    return mark.endMs - mark.startMs + 1
  }

  override fun getContentDuration(): Long = duration

  override fun getCurrentPosition(): Long {
    val pos = super.getCurrentPosition()
    val mark = currentMark() ?: return pos
    if (pos == C.TIME_UNSET) return pos
    return (pos - mark.startMs).coerceAtLeast(0L)
  }

  override fun getContentPosition(): Long = currentPosition

  override fun getBufferedPosition(): Long {
    val buf = super.getBufferedPosition()
    val mark = currentMark() ?: return buf
    if (buf == C.TIME_UNSET) return buf
    return (buf - mark.startMs).coerceAtLeast(0L)
  }

  override fun seekTo(positionMs: Long) {
    val mark = currentMark()
    if (mark == null) {
      super.seekTo(positionMs)
    } else {
      super.seekTo(mark.startMs + positionMs.coerceAtLeast(0L))
    }
  }
}
