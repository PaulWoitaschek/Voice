package voice.core.playback

import voice.core.data.Book
import java.time.Instant

/**
 * A book that was never played has no speed of its own yet, so it starts at [speed], the one the listener last
 * listened at. [speed] is only read for such a book. The player keeps it as the book's own speed once it loads the
 * book.
 */
inline fun Book.withDefaultSpeed(speed: () -> Float): Book {
  return if (content.lastPlayedAt == Instant.EPOCH) {
    update { it.copy(playbackSpeed = speed()) }
  } else {
    this
  }
}
