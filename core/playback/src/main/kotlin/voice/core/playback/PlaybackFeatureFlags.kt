package voice.core.playback

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
annotation class ExperimentalPlaybackPersistenceQualifier

@Qualifier
annotation class Media3AudioOffloadFeatureFlagQualifier

@BindingContainer
@ContributesTo(AppScope::class)
object PlaybackFeatureFlags {

  @Provides
  @SingleIn(AppScope::class)
  @ExperimentalPlaybackPersistenceQualifier
  fun experimentalPlaybackPersistenceQualifier(factory: FeatureFlagFactory): FeatureFlag<Boolean> {
    return factory.boolean(
      key = "experimental_playback_persistence",
      description = "Uses the experimental playback persistence implementation.",
    )
  }

  @Provides
  @IntoSet
  fun experimentalPlaybackPersistenceIntoSet(@ExperimentalPlaybackPersistenceQualifier flag: FeatureFlag<Boolean>): FeatureFlag<*> = flag

  @Provides
  @SingleIn(AppScope::class)
  @Media3AudioOffloadFeatureFlagQualifier
  fun media3AudioOffloadFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> {
    return factory.boolean(
      key = "media3_audio_offload",
      description = "Uses Media3 audio offload when the device supports it.",
    )
  }

  @Provides
  @IntoSet
  fun media3AudioOffloadFeatureFlagIntoSet(@Media3AudioOffloadFeatureFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<*> = flag
}
