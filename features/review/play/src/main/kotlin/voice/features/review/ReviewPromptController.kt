package voice.features.review

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.core.analytics.api.Analytics
import voice.core.common.AppInfoProvider
import voice.core.data.Book
import voice.core.data.ReviewPromptState
import voice.core.data.repo.BookRepository
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.repo.ListeningSummary
import voice.core.data.store.ReviewPromptStateStore
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.ReviewEnabledFeatureFlagQualifier
import voice.core.featureflag.ReviewPromptForceFeatureFlagQualifier
import voice.core.playback.playstate.PlayStateManager
import java.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaInstant

/**
 * Finds out whether now is a good moment for the rating prompt and keeps track of what the listener answered.
 */
@SingleIn(AppScope::class)
@Inject
class ReviewPromptController(
  @ReviewPromptStateStore
  private val store: DataStore<ReviewPromptState>,
  private val listeningStatsRepo: ListeningStatsRepo,
  private val bookRepository: BookRepository,
  private val playStateManager: PlayStateManager,
  private val appInfoProvider: AppInfoProvider,
  private val clock: Clock,
  private val analytics: Analytics,
  @ReviewEnabledFeatureFlagQualifier
  private val enabled: FeatureFlag<Boolean>,
  @ReviewPromptForceFeatureFlagQualifier
  private val force: FeatureFlag<Boolean>,
  private val scope: CoroutineScope,
) {

  private var forcedShown = false

  // shown but not answered yet, so it comes back when the activity is recreated
  private var showing: ReviewPrompt? = null

  internal val versionName: String get() = appInfoProvider.versionName

  /**
   * The prompt to show now, if any. Showing it counts as an ask right away, so it never comes back
   * too soon, even when the app gets closed while it's open.
   */
  internal suspend fun prompt(): ReviewPrompt? {
    showing?.let { return it }
    return newPrompt()?.also { showing = it }
  }

  private suspend fun newPrompt(): ReviewPrompt? {
    // never interrupt a story
    if (playStateManager.playState == PlayStateManager.PlayState.Playing) return null

    if (force.get() && !forcedShown) {
      forcedShown = true
      return forcedPrompt()
    }

    if (!enabled.get()) return null
    val state = store.data.first()
    if (state.done) return null

    val books = bookRepository.all()
    val summary = listeningStatsRepo.summary()
    val signals = signals(books, summary)
    val trigger = ReviewPromptPolicy.trigger(state, signals) ?: return null
    store.updateData { ReviewPromptPolicy.asked(it, signals) }
    analytics.event(
      name = "review_prompt_shown",
      params = mapOf(
        "trigger" to trigger.analyticsName,
        "ask" to (state.asks + 1).toString(),
      ),
    )
    return prompt(trigger, books, summary, forced = false)
  }

  internal fun answer(
    prompt: ReviewPrompt,
    answer: ReviewAnswer,
  ) {
    showing = null
    if (prompt.forced) return
    val now = clock.instant()
    scope.launch {
      store.updateData { ReviewPromptPolicy.answered(it, answer, now) }
    }
    analytics.event(name = "review_prompt_answer", params = mapOf("answer" to answer.analyticsName))
  }

  private suspend fun forcedPrompt(): ReviewPrompt {
    val books = bookRepository.all()
    val summary = listeningStatsRepo.summary()
    val finished = summary.finishedBooks.firstOrNull { finished -> books.any { it.id == finished.bookId } }
    val trigger = if (finished != null) {
      ReviewTrigger.BookFinished(finished.bookId)
    } else {
      ReviewTrigger.Milestone(ReviewPromptPolicy.MILESTONE_HOURS.first())
    }
    return prompt(trigger, books, summary, forced = true)
  }

  private fun signals(
    books: List<Book>,
    summary: ListeningSummary,
  ): ReviewSignals {
    val existing = books.mapTo(mutableSetOf()) { it.id }
    return ReviewSignals(
      now = clock.instant(),
      installedAt = appInfoProvider.installTime.toJavaInstant(),
      listened = summary.listened,
      listeningDays = summary.listeningDays,
      // a book that's gone since is nothing to celebrate
      finishedBooks = summary.finishedBooks.filter { it.bookId in existing },
    )
  }

  private fun prompt(
    trigger: ReviewTrigger,
    books: List<Book>,
    summary: ListeningSummary,
    forced: Boolean,
  ): ReviewPrompt {
    val headline = when (trigger) {
      is ReviewTrigger.BookFinished -> {
        val book = books.first { it.id == trigger.bookId }
        ReviewPrompt.Headline.BookFinished(name = book.content.name, cover = book.content.coverUrl)
      }
      is ReviewTrigger.Milestone -> ReviewPrompt.Headline.Milestone
    }
    return ReviewPrompt(headline = headline, stats = stats(books, summary), forced = forced)
  }

  /**
   * Taken from the library rather than the listening sessions, so listeners who were around long
   * before Voice started to count see all they listened to.
   */
  private fun stats(
    books: List<Book>,
    summary: ListeningSummary,
  ): ReviewPrompt.Stats {
    val started = books.filter { it.position > 0 }
    val inLibrary = started.sumOf { it.position }.milliseconds
    return ReviewPrompt.Stats(
      listenedHours = maxOf(inLibrary, summary.listened).inWholeHours.toInt().coerceAtLeast(1),
      finishedBooks = started.count { it.position >= it.duration - FINISHED_WITHIN.inWholeMilliseconds },
      authors = started.mapNotNullTo(mutableSetOf()) { it.content.author?.trim()?.lowercase()?.ifEmpty { null } }.size,
    )
  }

  private companion object {
    // the same as the finished category in the library
    val FINISHED_WITHIN = 5.seconds
  }
}

private val ReviewTrigger.analyticsName: String
  get() = when (this) {
    is ReviewTrigger.BookFinished -> "book_finished"
    is ReviewTrigger.Milestone -> "milestone_$hours"
  }

private val ReviewAnswer.analyticsName: String
  get() = when (this) {
    ReviewAnswer.Rate -> "rate"
    ReviewAnswer.Idea -> "idea"
    ReviewAnswer.Bug -> "bug"
    ReviewAnswer.Email -> "email"
    ReviewAnswer.Later -> "later"
  }
