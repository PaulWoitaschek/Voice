package voice.features.review

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.FeatureFlagFactory

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<FeatureFlag<*>>())
class ReviewEnabledFeatureFlag(flag: FeatureFlag<Boolean>) : FeatureFlag<Boolean> by flag {

  @Inject
  constructor(factory: FeatureFlagFactory) : this(
    factory.boolean(
      key = "review_enabled",
      description = "Shows the rating prompt after a finished book or a listening milestone.",
      defaultValue = false,
    ),
  )
}
