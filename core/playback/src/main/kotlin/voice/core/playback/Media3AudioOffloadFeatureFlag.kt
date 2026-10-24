package voice.core.playback

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.FeatureFlagFactory

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<FeatureFlag<*>>())
class Media3AudioOffloadFeatureFlag(flag: FeatureFlag<Boolean>) : FeatureFlag<Boolean> by flag {

  @Inject
  constructor(factory: FeatureFlagFactory) : this(
    factory.boolean(
      key = "media3_audio_offload",
      description = "Uses Media3 audio offload when the device supports it.",
    ),
  )
}
