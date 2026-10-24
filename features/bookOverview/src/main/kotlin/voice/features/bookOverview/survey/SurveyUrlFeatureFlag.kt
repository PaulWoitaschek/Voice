package voice.features.bookOverview.survey

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.FeatureFlagFactory

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<FeatureFlag<*>>())
class SurveyUrlFeatureFlag(flag: FeatureFlag<String>) : FeatureFlag<String> by flag {

  @Inject
  constructor(factory: FeatureFlagFactory) : this(
    factory.string(
      key = "survey_url",
      description = "The URL of a survey the library invites to, ahead of the one built into the app. Empty leaves only the built-in one.",
      defaultValue = "",
    ),
  )
}
