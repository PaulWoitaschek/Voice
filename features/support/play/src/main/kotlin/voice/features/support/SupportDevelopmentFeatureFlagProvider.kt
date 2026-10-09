package voice.features.support

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.FeatureFlagFactory
import voice.core.featureflag.SupportDevelopmentFeatureFlagQualifier

@BindingContainer
@ContributesTo(AppScope::class)
object SupportDevelopmentFeatureFlagProvider {

  @Provides
  @SingleIn(AppScope::class)
  @SupportDevelopmentFeatureFlagQualifier
  fun supportDevelopmentFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> {
    return factory.boolean(
      key = "support_development",
      description = "Shows the support card in the settings. Turned on remotely once the supporter products are live.",
      defaultValue = false,
    )
  }
}
