package voice.features.bookOverview.support

import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import voice.core.analytics.api.Analytics
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.supporter.SupporterStatusStore

@Inject
class SupportCard(
  private val enabled: SupportCardFeatureFlag,
  private val answeredStore: SupportCardAnsweredStore,
  private val supporterStatusStore: SupporterStatusStore,
  private val statsRepo: ListeningStatsRepo,
  private val clock: Clock,
  private val analytics: Analytics,
) {

  fun show(): Flow<Boolean> {
    return combine(enabled.flow, answeredStore.data, supporterStatusStore.data) {
      enabled,
      answered,
      supporter ->
      enabled.value && !answered && supporter.badge == null && finishedBookLately()
    }
  }

  // the same bar as the rating prompt, so Voice only asks listeners who know it
  private suspend fun finishedBookLately(): Boolean {
    val summary = statsRepo.summary()
    val finished = summary.finishedBooks.firstOrNull() ?: return false
    return finished.at >= clock.now() - 3.days &&
      summary.listened >= 5.hours &&
      summary.listeningDays >= 3
  }

  fun shown() {
    analytics.event("support_card", mapOf("action" to "show"))
  }

  suspend fun open() {
    answeredStore.updateData { true }
    analytics.event("support_open", mapOf("from" to "library_card"))
  }

  suspend fun dismiss() {
    answeredStore.updateData { true }
    analytics.event("support_card", mapOf("action" to "dismiss"))
  }
}
