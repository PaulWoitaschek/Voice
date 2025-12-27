package voice.core.featureflag

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn

@BindingContainer
@ContributesTo(AppScope::class)
object FeatureFlagBindingContainer {

  @Provides
  @SingleIn(AppScope::class)
  @ReviewEnabledFeatureFlagQualifier
  fun reviewEnabledFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> = factory.boolean("review_enabled", defaultValue = false)

  @Provides
  @SingleIn(AppScope::class)
  @UserAgentFeatureFlagQualifier
  fun userAgentFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<String> {
    return factory.string(key = "user_agent", defaultValue = "Mozilla/5.0")
  }

  @Provides
  @SingleIn(AppScope::class)
  @FolderPickerInSettingsFeatureFlagQualifier
  fun folderPickerInSettingsFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> {
    return factory.boolean(key = "folder_picker_in_settings")
  }
}

@Qualifier
annotation class ReviewEnabledFeatureFlagQualifier

@Qualifier
annotation class UserAgentFeatureFlagQualifier

@Qualifier
annotation class FolderPickerInSettingsFeatureFlagQualifier
