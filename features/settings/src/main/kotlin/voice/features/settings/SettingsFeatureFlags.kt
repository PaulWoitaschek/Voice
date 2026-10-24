package voice.features.settings

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import voice.core.common.AppInfoProvider
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.FeatureFlagFactory

@Qualifier
annotation class SupportDevelopmentFeatureFlagQualifier

@BindingContainer
@ContributesTo(AppScope::class)
object SettingsFeatureFlags {

  @Provides
  @SingleIn(AppScope::class)
  @SupportDevelopmentFeatureFlagQualifier
  fun supportDevelopmentFeatureFlag(
    factory: FeatureFlagFactory,
    appInfoProvider: AppInfoProvider,
  ): FeatureFlag<Boolean> {
    return factory.boolean(
      key = "support_development",
      description = "Shows the support card in the settings. Play builds turn it on remotely once the supporter products are live.",
      defaultValue = appInfoProvider.supportDevelopmentIncluded,
    )
  }

  @Provides
  @IntoSet
  fun supportDevelopmentFeatureFlagIntoSet(@SupportDevelopmentFeatureFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<*> = flag
}
