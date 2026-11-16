package voice.features.review

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import voice.core.data.BookId
import voice.core.data.repo.FinishedBook

/** What the policy needs to know about the listener. */
internal data class ReviewSignals(
  val now: Instant,
  val installedAt: Instant,
  val listened: Duration,
  val listeningDays: Int,
  /** Newest first. */
  val finishedBooks: List<FinishedBook>,
)

/** The win the prompt celebrates. */
internal sealed interface ReviewTrigger {
  data class BookFinished(val bookId: BookId) : ReviewTrigger

  data class Milestone(val listened: Duration) : ReviewTrigger
}

internal enum class ReviewAnswer {
  Rate,
  Idea,
  Bug,
  Email,
  Later,
}

/**
 * Decides when to ask for a rating: after a win, once the listener got to know Voice, and never
 * pushy.
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
  val MILESTONES = listOf(5, 10, 25, 50, 100, 250, 500, 1000).map { it.hours }
  val SNOOZE = 30.days
  val LONG_SNOOZE = 90.days

  fun trigger(
    state: ReviewPromptState,
    signals: ReviewSignals,
  ): ReviewTrigger? {
    if (state.done || state.asks >= MAX_ASKS) return null
    if (signals.now < signals.installedAt + MIN_INSTALL_AGE) return null
    if (signals.listened < MIN_LISTENED || signals.listeningDays < MIN_LISTENING_DAYS) return null
    val snoozedUntil = state.snoozedUntil
    if (snoozedUntil != null && signals.now < snoozedUntil) return null
    if (state.asks > 0 && signals.listened - state.listenedAtLastAsk < LISTENED_BETWEEN_ASKS)
      return null

    val lastAsked = state.lastAskedAt ?: Instant.DISTANT_PAST
    val finished =
      signals.finishedBooks.firstOrNull {
        it.at > lastAsked && it.at >= signals.now - RECENTLY_FINISHED
      }
    if (finished != null) return ReviewTrigger.BookFinished(finished.bookId)

    val milestone = reachedMilestone(signals.listened)
    if (milestone > state.celebratedMilestone) return ReviewTrigger.Milestone(milestone)
    return null
  }

  /** The prompt is showing. Until there's an answer, it counts as one to ask later. */
  fun asked(
    state: ReviewPromptState,
    signals: ReviewSignals,
  ): ReviewPromptState {
    return state.copy(
      asks = state.asks + 1,
      lastAskedAt = signals.now,
      listenedAtLastAsk = signals.listened,
      celebratedMilestone = maxOf(state.celebratedMilestone, reachedMilestone(signals.listened)),
      snoozedUntil = signals.now + SNOOZE,
    )
  }

  fun answered(
    state: ReviewPromptState,
    answer: ReviewAnswer,
    now: Instant,
  ): ReviewPromptState {
    val snooze =
      when (answer) {
        ReviewAnswer.Rate -> return state.copy(done = true)
        ReviewAnswer.Idea,
        ReviewAnswer.Bug,
        ReviewAnswer.Email -> LONG_SNOOZE
        ReviewAnswer.Later -> if (state.asks >= 2) LONG_SNOOZE else SNOOZE
      }
    return state.copy(snoozedUntil = now + snooze)
  }

  private fun reachedMilestone(listened: Duration): Duration {
    return MILESTONES.lastOrNull { listened >= it } ?: Duration.ZERO
  }
}
