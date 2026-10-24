package voice.core.kioskmode

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
annotation class KioskModeFeatureFlagQualifier

@BindingContainer
@ContributesTo(AppScope::class)
object KioskModeFeatureFlags {

  @Provides
  @SingleIn(AppScope::class)
  @KioskModeFeatureFlagQualifier
  fun kioskModeFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> {
    return factory.boolean(
      key = "kiosk_mode",
      description = "Shows demo content on the overview, playback, and bookmark screens.",
    )
  }

  @Provides
  @IntoSet
  fun kioskModeFeatureFlagIntoSet(@KioskModeFeatureFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<*> = flag
}
