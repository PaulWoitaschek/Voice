package voice.core.data.sleeptimer

import kotlinx.serialization.Serializable
import voice.core.data.BookId
import kotlin.time.Duration

/**
 * Stored when a timed sleep timer runs out, so that the next time the app is opened the user can
 * rewind to the point where they might have fallen asleep.
 */
@Serializable
public data class SleepTimerRewind(
  val bookId: BookId,
  /**
   * Position in the whole book (not the chapter) when the countdown started.
   */
  val startPositionInBookMs: Long,
  /**
   * How long the timer counted down. The timer only counts while playing.
   */
  val timerDuration: Duration,
  val playbackSpeed: Float,
)
