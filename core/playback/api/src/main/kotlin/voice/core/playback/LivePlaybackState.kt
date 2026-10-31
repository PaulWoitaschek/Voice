package voice.core.playback

import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.ChapterId

data class LivePlaybackState(
  val bookId: BookId,
  val chapterId: ChapterId,
  val positionMs: Long,
  val isPlaying: Boolean,
  val playbackSpeed: Float,
)

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
