package voice.features.support

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import voice.core.common.AppInfoProvider
import voice.core.common.AppVariant
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.FeatureFlagFactory

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<FeatureFlag<*>>())
class SupporterCountFeatureFlag(flag: FeatureFlag<Long>) : FeatureFlag<Long> by flag {

  @Inject
  constructor(
    factory: FeatureFlagFactory,
    appInfoProvider: AppInfoProvider,
  ) : this(
    factory.long(
      key = "supporter_count",
      description =
        "How many listeners have supported Voice, rounded down. Shown on the support sheet. Hidden when 0.",
      defaultValue =
        when (appInfoProvider.variant) {
          AppVariant.Foss -> 200
          AppVariant.Play -> 0
        },
    ),
  )
}
