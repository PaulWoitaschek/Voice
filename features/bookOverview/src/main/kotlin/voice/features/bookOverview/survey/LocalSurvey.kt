package voice.features.bookOverview.survey

import dev.zacsweers.metro.Inject

/**
 * The survey built into the app, which also reaches builds without Remote Config. When it runs, like
 * the days or the versions, is written here by hand.
 */
@Inject
class LocalSurvey {

  suspend fun url(): String? = null
}
