package voice.features.bookOverview.survey

import dev.zacsweers.metro.Inject

/**
 * The survey built into the app, which also reaches builds without Remote Config. Its URL and when it
 * runs, such as the dates or app versions, are set here in code.
 */
@Inject
class LocalSurvey {

  suspend fun url(): String? = null
}
