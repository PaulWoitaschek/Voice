package voice.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A stretch of listening to one book, kept on the device for listening stats.
 *
 * Short breaks don't end a session: playing on within a few minutes continues it. Only the time
 * actually spent playing counts towards [listenedMillis].
 */
@Entity(
  tableName = "listening_session",
  indices = [
    Index(value = ["startedAtMillis"]),
    Index(value = ["bookId"]),
  ],
)
public data class ListeningSession(
  val bookId: BookId,
  val startedAtMillis: Long,
  val endedAtMillis: Long,
  /**
   * The time spent playing.
   */
  val listenedMillis: Long,
  /**
   * How far the book moved on while playing. More than [listenedMillis] when playing faster.
   */
  val audioMillis: Long,
  /**
   * The offset of the time zone the session started in, so local days and hours can be told apart later.
   */
  val utcOffsetSeconds: Int,
  /**
   * Whether playback reached the end of the book.
   */
  val reachedEnd: Boolean,
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
)
