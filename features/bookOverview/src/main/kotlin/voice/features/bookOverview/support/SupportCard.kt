package voice.features.bookOverview.support

import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import voice.core.analytics.api.Analytics
import voice.core.data.BookId
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.supporter.SupporterStatusStore

sealed interface SupportCardOffer {
  data object NotNow : SupportCardOffer

  data class Now(
    val look: SupportCardLook,
    val finishedBook: BookId,
    val listened: Duration,
  ) : SupportCardOffer
}

@Inject
class SupportCard(
  private val look: SupportCardFeatureFlag,
  private val answeredStore: SupportCardAnsweredStore,
  private val supporterStatusStore: SupporterStatusStore,
  private val statsRepo: ListeningStatsRepo,
  private val clock: Clock,
  private val analytics: Analytics,
) {

  // the setting can change while a card shows, and the reports name the look the listener got
  private var offered = SupportCardLook.None

  fun offer(): Flow<SupportCardOffer> {
    return combine(look.flow, answeredStore.data, supporterStatusStore.data) {
      look,
      answered,
      supporter ->
      if (answered || supporter.badge != null) return@combine SupportCardOffer.NotNow
      val summary = statsRepo.summary()
      val finished = summary.finishedBooks.firstOrNull()
      // the same bar as the rating prompt, so Voice only asks listeners who know it
      if (
        finished == null ||
          finished.at < clock.now() - 3.days ||
          summary.listened < 5.hours ||
          summary.listeningDays < 3
      ) {
        return@combine SupportCardOffer.NotNow
      }
      offered = look.value
      SupportCardOffer.Now(look.value, finished.bookId, summary.listened)
    }
  }

  fun shown() {
    report("show")
  }

  // Voice compares the cards with the listeners who could have seen one
  fun heldBack() {
    report("skip")
  }

  suspend fun open() {
    answeredStore.updateData { true }
    analytics.event("support_open", mapOf("from" to "library_card"))
  }

  suspend fun dismiss() {
    answeredStore.updateData { true }
    report("dismiss")
  }

  private fun report(action: String) {
    analytics.event(
      "support_card",
      mapOf("action" to action, "kind" to offered.name.lowercase()),
    )
  }
}
