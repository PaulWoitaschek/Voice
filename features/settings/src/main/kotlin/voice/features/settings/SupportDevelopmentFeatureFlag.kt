package voice.features.settings

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import voice.core.common.AppInfoProvider
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.FeatureFlagFactory

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<FeatureFlag<*>>())
class SupportDevelopmentFeatureFlag(flag: FeatureFlag<Boolean>) : FeatureFlag<Boolean> by flag {

  @Inject
  constructor(
    factory: FeatureFlagFactory,
    appInfoProvider: AppInfoProvider,
  ) : this(
    factory.boolean(
      key = "support_development",
      description = "Shows the support card in the settings. Play builds turn it on remotely once the supporter products are live.",
      defaultValue = appInfoProvider.supportDevelopmentIncluded,
    ),
  )
}
