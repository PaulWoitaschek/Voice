package voice.features.review

import voice.core.data.BookId
import voice.core.data.ReviewPromptState
import voice.core.data.repo.FinishedBook
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class ReviewPromptPolicyTest {

  private val installedAt = Instant.parse("2026-01-01T00:00:00Z")
  private val now = installedAt + 30.days
  private val dune = BookId("dune")

  private fun signals(
    now: Instant = this.now,
    listened: Duration = 7.hours,
    listeningDays: Int = 4,
    finishedBooks: List<FinishedBook> = emptyList(),
  ) = ReviewSignals(
    now = now,
    installedAt = installedAt,
    listened = listened,
    listeningDays = listeningDays,
    finishedBooks = finishedBooks,
  )

  private fun trigger(
    state: ReviewPromptState = ReviewPromptState(),
    signals: ReviewSignals = signals(),
  ) = ReviewPromptPolicy.trigger(state, signals)

  @Test
  fun `a milestone is celebrated once the listener got to know Voice`() {
    assertEquals(expected = ReviewTrigger.Milestone(5), actual = trigger())
  }

  @Test
  fun `the highest milestone reached is celebrated`() {
    assertEquals(expected = ReviewTrigger.Milestone(25), actual = trigger(signals = signals(listened = 31.hours)))
  }

  @Test
  fun `nothing is asked right after installing`() {
    assertNull(trigger(signals = signals(now = installedAt + 2.days)))
  }

  @Test
  fun `nothing is asked before listening for a while`() {
    assertNull(trigger(signals = signals(listened = 4.hours)))
  }

  @Test
  fun `nothing is asked before listening on a few days`() {
    assertNull(trigger(signals = signals(listeningDays = 2)))
  }

  @Test
  fun `a book finished recently is celebrated rather than a milestone`() {
    val finished = FinishedBook(dune, at = now - 1.days)

    assertEquals(
      expected = ReviewTrigger.BookFinished(dune),
      actual = trigger(signals = signals(finishedBooks = listOf(finished))),
    )
  }

  @Test
  fun `a book finished a while ago is no reason to celebrate`() {
    val finished = FinishedBook(dune, at = now - 4.days)

    assertEquals(
      expected = ReviewTrigger.Milestone(5),
      actual = trigger(signals = signals(finishedBooks = listOf(finished))),
    )
  }

  @Test
  fun `a celebrated milestone is not celebrated again`() {
    val state = ReviewPromptState(celebratedMilestoneHours = 5)

    assertNull(trigger(state))
  }

  @Test
  fun `showing the prompt snoozes it and remembers what was celebrated`() {
    val signals = signals(listened = 12.hours)

    val state = ReviewPromptPolicy.asked(ReviewPromptState(), signals)

    assertEquals(
      expected = ReviewPromptState(
        asks = 1,
        lastAskedAtMillis = now.toEpochMilli(),
        listenedAtLastAskMillis = 12.hours.inWholeMilliseconds,
        celebratedMilestoneHours = 10,
        snoozedUntilMillis = (now + 30.days).toEpochMilli(),
      ),
      actual = state,
    )
  }

  @Test
  fun `after an ask, it waits for the snooze and more listening`() {
    val asked = ReviewPromptPolicy.asked(ReviewPromptState(), signals(listened = 7.hours))
    val finishedLater = listOf(FinishedBook(dune, at = now + 40.days))

    // still snoozed
    assertNull(trigger(asked, signals(now = now + 29.days, listened = 20.hours, finishedBooks = finishedLater)))
    // not listened since
    assertNull(trigger(asked, signals(now = now + 41.days, listened = 11.hours, finishedBooks = finishedLater)))
    assertEquals(
      expected = ReviewTrigger.BookFinished(dune),
      actual = trigger(asked, signals(now = now + 41.days, listened = 12.hours, finishedBooks = finishedLater)),
    )
  }

  @Test
  fun `a book finished before the last ask is not celebrated again`() {
    val state = ReviewPromptState(asks = 1, lastAskedAtMillis = (now - 1.days).toEpochMilli())
    val finished = listOf(FinishedBook(dune, at = now - 2.days))

    assertEquals(
      expected = ReviewTrigger.Milestone(5),
      actual = trigger(state, signals(finishedBooks = finished)),
    )
  }

  @Test
  fun `rating ends the asking for good`() {
    val asked = ReviewPromptPolicy.asked(ReviewPromptState(), signals())

    val rated = ReviewPromptPolicy.answered(asked, ReviewAnswer.Rate, now)

    assertEquals(expected = asked.copy(done = true), actual = rated)
    assertNull(trigger(rated, signals(now = now + 365.days, listened = 1000.hours)))
  }

  @Test
  fun `feedback snoozes for longer than not now`() {
    val asked = ReviewPromptPolicy.asked(ReviewPromptState(), signals())

    assertEquals(
      expected = (now + 90.days).toEpochMilli(),
      actual = ReviewPromptPolicy.answered(asked, ReviewAnswer.Idea, now).snoozedUntilMillis,
    )
    assertEquals(
      expected = (now + 30.days).toEpochMilli(),
      actual = ReviewPromptPolicy.answered(asked, ReviewAnswer.Later, now).snoozedUntilMillis,
    )
  }

  @Test
  fun `not now snoozes for longer from the second ask on`() {
    val state = ReviewPromptState(asks = 2)

    assertEquals(
      expected = (now + 90.days).toEpochMilli(),
      actual = ReviewPromptPolicy.answered(state, ReviewAnswer.Later, now).snoozedUntilMillis,
    )
  }

  @Test
  fun `nobody is asked more than three times`() {
    val state = ReviewPromptState(asks = 3)

    assertNull(trigger(state, signals(listened = 1000.hours)))
  }
}

private operator fun Instant.plus(duration: Duration): Instant = plus(duration.toJavaDuration())

private operator fun Instant.minus(duration: Duration): Instant = minus(duration.toJavaDuration())
