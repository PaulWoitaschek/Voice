package voice.features.bookOverview.survey

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.FeatureFlagFactory

@Qualifier
annotation class SurveyUrlFeatureFlagQualifier

@BindingContainer
@ContributesTo(AppScope::class)
object SurveyFeatureFlags {

  @Provides
  @SingleIn(AppScope::class)
  @SurveyUrlFeatureFlagQualifier
  fun surveyUrlFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<String> {
    return factory.string(
      key = "survey_url",
      description = "The URL of a survey the library invites to, ahead of the one built into the app. Empty leaves only the built-in one.",
      defaultValue = "",
    )
  }

  @Provides
  @IntoSet
  fun surveyUrlFeatureFlagIntoSet(@SurveyUrlFeatureFlagQualifier flag: FeatureFlag<String>): FeatureFlag<*> = flag
}
