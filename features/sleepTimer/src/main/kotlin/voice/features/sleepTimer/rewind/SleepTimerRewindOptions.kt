package voice.features.sleepTimer.rewind

import voice.core.data.Book
import voice.core.data.ChapterId
import voice.core.data.sleeptimer.SleepTimerRewind
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal data class RewindOption(
  /** How far back to go, measured in sleep timer time. */
  val amount: Duration,
  val positionInBookMs: Long,
  val isTimerStart: Boolean,
)

private const val MAX_OPTIONS = 6

/**
 * The first option goes back to where the timer started, followed by evenly spaced shorter amounts:
 * 30 min → 30, 25, 20, 15, 10, 5 and 60 min → 60, 50, 40, 30, 20, 10.
 */
internal fun SleepTimerRewind.rewindOptions(): List<RewindOption> {
  val step = rewindStep(timerDuration)
  val shorterAmounts = generateSequence(step * ((timerDuration - 1.minutes) / step).toInt()) { it - step }
    .takeWhile { it > Duration.ZERO }
    .take(MAX_OPTIONS - 1)
  return (sequenceOf(timerDuration) + shorterAmounts)
    .map { amount ->
      // the timer only counts while playing, so it runs at wall clock speed while the book runs at playback speed
      val listenedAfterRewind = (timerDuration - amount) * playbackSpeed.toDouble()
      RewindOption(
        amount = amount,
        positionInBookMs = startPositionInBookMs + listenedAfterRewind.inWholeMilliseconds,
        isTimerStart = amount == timerDuration,
      )
    }
    .toList()
}

private fun rewindStep(timerDuration: Duration): Duration {
  val minutesPerOption = timerDuration.inWholeMinutes.toDouble() / MAX_OPTIONS
  val roundedUpToFive = (ceil(minutesPerOption / 5) * 5).toInt().coerceAtLeast(5)
  return roundedUpToFive.minutes
}

internal data class ChapterPosition(
  val chapterId: ChapterId,
  val positionInChapterMs: Long,
)

internal fun Book.chapterPosition(positionInBookMs: Long): ChapterPosition {
  var remaining = positionInBookMs.coerceAtLeast(0)
  chapters.forEach { chapter ->
    if (remaining < chapter.duration) {
      return ChapterPosition(chapter.id, remaining)
    }
    remaining -= chapter.duration
  }
  val lastChapter = chapters.last()
  return ChapterPosition(lastChapter.id, lastChapter.duration)
}
