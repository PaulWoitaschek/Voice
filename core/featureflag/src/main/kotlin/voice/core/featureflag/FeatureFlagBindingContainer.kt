package voice.core.featureflag

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface FeatureFlagBindingContainer {

  @Provides
  @SingleIn(AppScope::class)
  @ReviewEnabledFeatureFlagQualifier
  fun reviewEnabledFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> {
    return factory.boolean(
      key = "review_enabled",
      description = "Shows the in-app review prompt when the review conditions are met.",
      defaultValue = false,
    )
  }

  @Binds
  @IntoSet
  fun bindReviewEnabledFeatureFlag(@ReviewEnabledFeatureFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<*>

  @Provides
  @SingleIn(AppScope::class)
  @UserAgentFeatureFlagQualifier
  fun userAgentFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<String> {
    return factory.string(
      key = "user_agent",
      description = "Overrides the HTTP user agent used for cover downloads.",
      defaultValue = "Mozilla/5.0",
    )
  }

  @Provides
  @SingleIn(AppScope::class)
  @FolderPickerInSettingsFeatureFlagQualifier
  fun folderPickerInSettingsFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> {
    return factory.boolean(
      key = "folder_picker_in_settings",
      description = "Shows the folder picker entry directly in settings.",
    )
  }

  @Binds
  @IntoSet
  fun bindFolderPickerInSettingsFeatureFlag(@FolderPickerInSettingsFeatureFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<*>

  @Provides
  @SingleIn(AppScope::class)
  @ExperimentalPlaybackPersistenceQualifier
  fun experimentalPlaybackPersistenceQualifier(factory: FeatureFlagFactory): FeatureFlag<Boolean> {
    return factory.boolean(
      key = "experimental_playback_persistence",
      description = "Uses the experimental playback persistence implementation.",
    )
  }

  @Binds
  @IntoSet
  fun bindExperimentalPlaybackPersistenceFeatureFlag(@ExperimentalPlaybackPersistenceQualifier flag: FeatureFlag<Boolean>): FeatureFlag<*>

  @Provides
  @SingleIn(AppScope::class)
  @Media3AudioOffloadFeatureFlagQualifier
  fun media3AudioOffloadFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> {
    return factory.boolean(
      key = "media3_audio_offload",
      description = "Uses Media3 audio offload when the device supports it.",
    )
  }

  @Binds
  @IntoSet
  fun bindMedia3AudioOffloadFeatureFlag(@Media3AudioOffloadFeatureFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<*>
}

@Qualifier
annotation class ReviewEnabledFeatureFlagQualifier

@Qualifier
annotation class UserAgentFeatureFlagQualifier

@Qualifier
annotation class FolderPickerInSettingsFeatureFlagQualifier

@Qualifier
annotation class ExperimentalPlaybackPersistenceQualifier

@Qualifier
annotation class Media3AudioOffloadFeatureFlagQualifier
