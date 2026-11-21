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
  // the player keeps its files when a scan finds that the book lost one of them
  return if (livePlaybackState.bookId == id && livePlaybackState.chapterId in content.chapters) {
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
