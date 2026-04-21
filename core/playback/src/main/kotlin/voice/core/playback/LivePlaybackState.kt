package voice.core.playback

import androidx.media3.common.C
import androidx.media3.session.MediaController
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.playback.session.MediaId
import voice.core.playback.session.toMediaIdOrNull

data class LivePlaybackState(
  val bookId: BookId,
  val chapterId: ChapterId,
  val positionMs: Long,
  val isPlaying: Boolean,
  val playbackSpeed: Float,
)

internal fun MediaController.livePlaybackStateSnapshot(bookId: BookId? = null): LivePlaybackState? {
  val mediaId = currentMediaItem?.mediaId?.toMediaIdOrNull() as? MediaId.Chapter ?: return null
  if (bookId != null && mediaId.bookId != bookId) {
    return null
  }
  val positionMs = currentPosition.takeUnless { it == C.TIME_UNSET || it < 0 } ?: return null
  return LivePlaybackState(
    bookId = mediaId.bookId,
    chapterId = mediaId.chapterId,
    positionMs = positionMs,
    isPlaying = isPlaying,
    playbackSpeed = playbackParameters.speed,
  )
}

fun Book.overlay(livePlaybackState: LivePlaybackState): Book {
  return if (livePlaybackState.bookId == id) {
    update {
      it.copy(
        currentChapter = livePlaybackState.chapterId,
        positionInChapter = livePlaybackState.positionMs,
        playbackSpeed = livePlaybackState.playbackSpeed,
      )
    }
  } else {
    this
  }
}
