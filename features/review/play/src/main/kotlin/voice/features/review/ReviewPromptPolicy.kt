package voice.features.review

import voice.core.data.BookId
import voice.core.data.ReviewPromptState
import voice.core.data.repo.FinishedBook
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

/**
 * What the policy needs to know about the listener.
 */
internal data class ReviewSignals(
  val now: Instant,
  val installedAt: Instant,
  val listened: Duration,
  val listeningDays: Int,
  /**
   * Newest first.
   */
  val finishedBooks: List<FinishedBook>,
)

/**
 * The win the prompt celebrates.
 */
internal sealed interface ReviewTrigger {
  data class BookFinished(val bookId: BookId) : ReviewTrigger
  data class Milestone(val hours: Int) : ReviewTrigger
}

internal enum class ReviewAnswer {
  Rate,
  Idea,
  Bug,
  Email,
  Later,
}

/**
 * Decides when to ask for a rating: after a win, once the listener got to know Voice, and never pushy.
 *
 * Everyone sees the same prompt with the same choices, whatever they think of Voice.
 */
internal object ReviewPromptPolicy {

  val MIN_INSTALL_AGE = 3.days
  val MIN_LISTENED = 5.hours
  const val MIN_LISTENING_DAYS = 3
  const val MAX_ASKS = 3

  // listening in between asks, so the prompt doesn't come back unless Voice is still in use
  val LISTENED_BETWEEN_ASKS = 5.hours

  // a book finished a while ago is no reason to celebrate anymore
  val RECENTLY_FINISHED = 3.days
  val MILESTONE_HOURS = listOf(5, 10, 25, 50, 100, 250, 500, 1000)
  val SNOOZE = 30.days
  val LONG_SNOOZE = 90.days

  fun trigger(
    state: ReviewPromptState,
    signals: ReviewSignals,
  ): ReviewTrigger? {
    if (state.done || state.asks >= MAX_ASKS) return null
    if (signals.now < signals.installedAt + MIN_INSTALL_AGE) return null
    if (signals.listened < MIN_LISTENED || signals.listeningDays < MIN_LISTENING_DAYS) return null
    val snoozedUntil = state.snoozedUntilMillis
    if (snoozedUntil != null && signals.now.toEpochMilli() < snoozedUntil) return null
    if (state.asks > 0 && signals.listened - state.listenedAtLastAskMillis.milliseconds < LISTENED_BETWEEN_ASKS) return null

    val lastAsked = state.lastAskedAtMillis?.let(Instant::ofEpochMilli) ?: Instant.EPOCH
    val finished = signals.finishedBooks.firstOrNull {
      it.at > lastAsked && it.at >= signals.now - RECENTLY_FINISHED
    }
    if (finished != null) return ReviewTrigger.BookFinished(finished.bookId)

    val milestone = reachedMilestone(signals.listened)
    if (milestone > state.celebratedMilestoneHours) return ReviewTrigger.Milestone(milestone)
    return null
  }

  /**
   * The prompt is showing. Until there's an answer, it counts as one to ask later.
   */
  fun asked(
    state: ReviewPromptState,
    signals: ReviewSignals,
  ): ReviewPromptState {
    val now = signals.now.toEpochMilli()
    return state.copy(
      asks = state.asks + 1,
      lastAskedAtMillis = now,
      listenedAtLastAskMillis = signals.listened.inWholeMilliseconds,
      celebratedMilestoneHours = maxOf(state.celebratedMilestoneHours, reachedMilestone(signals.listened)),
      snoozedUntilMillis = now + SNOOZE.inWholeMilliseconds,
    )
  }

  fun answered(
    state: ReviewPromptState,
    answer: ReviewAnswer,
    now: Instant,
  ): ReviewPromptState {
    val snooze = when (answer) {
      ReviewAnswer.Rate -> return state.copy(done = true)
      ReviewAnswer.Idea, ReviewAnswer.Bug, ReviewAnswer.Email -> LONG_SNOOZE
      ReviewAnswer.Later -> if (state.asks >= 2) LONG_SNOOZE else SNOOZE
    }
    return state.copy(snoozedUntilMillis = (now + snooze).toEpochMilli())
  }

  private fun reachedMilestone(listened: Duration): Int {
    return MILESTONE_HOURS.lastOrNull { listened >= it.hours } ?: 0
  }
}

private operator fun Instant.plus(duration: Duration): Instant = plus(duration.toJavaDuration())

private operator fun Instant.minus(duration: Duration): Instant = minus(duration.toJavaDuration())
