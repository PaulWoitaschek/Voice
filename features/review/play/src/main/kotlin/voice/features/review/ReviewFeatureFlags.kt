package voice.features.review

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
annotation class ReviewEnabledFeatureFlagQualifier

@Qualifier
annotation class ReviewPromptForceFeatureFlagQualifier

@BindingContainer
@ContributesTo(AppScope::class)
object ReviewFeatureFlags {

  @Provides
  @SingleIn(AppScope::class)
  @ReviewEnabledFeatureFlagQualifier
  fun reviewEnabledFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> {
    return factory.boolean(
      key = "review_enabled",
      description = "Shows the rating prompt after a finished book or a listening milestone.",
      defaultValue = false,
    )
  }

  @Provides
  @IntoSet
  fun reviewEnabledFeatureFlagIntoSet(@ReviewEnabledFeatureFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<*> = flag

  @Provides
  @SingleIn(AppScope::class)
  @ReviewPromptForceFeatureFlagQualifier
  fun reviewPromptForceFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> {
    return factory.boolean(
      key = "review_prompt_force",
      description = "Shows the rating prompt on the next visit to the library, ignoring its rules.",
    )
  }

  @Provides
  @IntoSet
  fun reviewPromptForceFeatureFlagIntoSet(@ReviewPromptForceFeatureFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<*> = flag
}
