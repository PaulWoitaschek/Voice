package voice.features.bookOverview.survey

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import voice.core.analytics.api.Analytics
import voice.core.common.AppInfoProvider
import java.net.URLEncoder
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.time.toJavaInstant

@Inject
class Survey(
  private val surveyUrlFeatureFlag: SurveyUrlFeatureFlag,
  private val handledSurveysStore: HandledSurveysStore,
  private val localSurvey: LocalSurvey,
  private val clock: Clock,
  private val appInfoProvider: AppInfoProvider,
  private val analytics: Analytics,
) {

  fun url(): Flow<String?> {
    return combine(surveyUrlFeatureFlag.flow, handledSurveysStore.data) { remoteUrl, handled ->
      // one a day at most, so dismissing a survey doesn't bring up the next one right away
      if (today() in handled.values) {
        null
      } else {
        listOfNotNull(remoteUrl.value.ifEmpty { null }, localSurvey.url())
          .firstOrNull { it !in handled }
      }
    }
  }

  suspend fun open(url: String): String {
    handled(url, action = "open")
    // named like the hidden fields of the Tally forms, which fill them from the query
    val params = listOf(
      "app_version" to appInfoProvider.versionName,
      "source" to (appInfoProvider.installer ?: "unknown"),
      "days_installed" to ChronoUnit.DAYS.between(appInfoProvider.installTime.toJavaInstant(), clock.instant()).toString(),
    ).joinToString(separator = "&") { (key, value) ->
      "$key=${URLEncoder.encode(value, "UTF-8")}"
    }
    val separator = if ('?' in url) '&' else '?'
    return "$url$separator$params"
  }

  suspend fun dismiss(url: String) {
    handled(url, action = "dismiss")
  }

  private suspend fun handled(
    url: String,
    action: String,
  ) {
    val today = today()
    handledSurveysStore.updateData { it + (url to today) }
    analytics.event(name = "survey_card", params = mapOf("action" to action))
  }

  private fun today(): Long = LocalDate.now(clock).toEpochDay()
}
