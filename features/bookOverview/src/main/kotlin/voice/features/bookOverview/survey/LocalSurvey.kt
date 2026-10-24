package voice.features.bookOverview.survey

import dev.zacsweers.metro.Inject
import voice.core.common.AppInfoProvider
import java.time.Clock
import java.time.LocalDate

/**
 * The survey built into the app, which also reaches builds without Remote Config. Its URL and when it
 * runs, such as the dates or app versions, are set here in code.
 */
@Inject
class LocalSurvey(
  private val appInfoProvider: AppInfoProvider,
  private val clock: Clock,
) {

  suspend fun url(): String? {
    // Play builds get their surveys from Remote Config
    val runs = appInfoProvider.flavor != "play" && LocalDate.now(clock) < LocalDate.of(2026, 11, 14)
    return if (runs) "https://tally.so/r/5XBPZN" else null
  }
}
