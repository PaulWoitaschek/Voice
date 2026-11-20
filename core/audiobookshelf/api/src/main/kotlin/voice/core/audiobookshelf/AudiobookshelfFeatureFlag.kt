package voice.core.audiobookshelf

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
class AudiobookshelfFeatureFlag(flag: FeatureFlag<Boolean>) : FeatureFlag<Boolean> by flag {

  @Inject
  constructor(
    factory: FeatureFlagFactory,
    appInfoProvider: AppInfoProvider,
  ) : this(
    factory.boolean(
      key = "audiobookshelf",
      description =
        "Lets listeners connect an Audiobookshelf server. A server that is connected stays, also when this is off.",
      defaultValue =
        when (appInfoProvider.variant) {
          AppVariant.Foss -> true
          AppVariant.Play -> false
        },
    ),
  )
}
