package voice.core.featureflag

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn

@BindingContainer
@ContributesTo(AppScope::class)
object FeatureFlagBindingContainer {

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
  @SingleIn(AppScope::class)
  @ReviewPromptForceFeatureFlagQualifier
  fun reviewPromptForceFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> {
    return factory.boolean(
      key = "review_prompt_force",
      description = "Shows the rating prompt on the next visit to the library, ignoring its rules.",
    )
  }

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
      defaultValue = false,
    )
  }

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
  @SingleIn(AppScope::class)
  @Media3AudioOffloadFeatureFlagQualifier
  fun media3AudioOffloadFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> {
    return factory.boolean(
      key = "media3_audio_offload",
      description = "Uses Media3 audio offload when the device supports it.",
    )
  }

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
  @SingleIn(AppScope::class)
  @SupporterNoteFeatureFlagQualifier
  fun supporterNoteFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<String> {
    return factory.string(
      key = "supporter_note",
      description = "A note on the support sheet, e.g. what the support paid for this month. Hidden when empty.",
      defaultValue = "",
    )
  }

  @Provides
  @ElementsIntoSet
  fun featureFlags(
    @ReviewEnabledFeatureFlagQualifier reviewEnabled: FeatureFlag<Boolean>,
    @ReviewPromptForceFeatureFlagQualifier reviewPromptForce: FeatureFlag<Boolean>,
    @FolderPickerInSettingsFeatureFlagQualifier folderPickerInSettings: FeatureFlag<Boolean>,
    @ExperimentalPlaybackPersistenceQualifier experimentalPlaybackPersistence: FeatureFlag<Boolean>,
    @Media3AudioOffloadFeatureFlagQualifier media3AudioOffload: FeatureFlag<Boolean>,
    @KioskModeFeatureFlagQualifier kioskMode: FeatureFlag<Boolean>,
    @SupportDevelopmentFeatureFlagQualifier supportDevelopment: FeatureFlag<Boolean>,
    @SupporterNoteFeatureFlagQualifier supporterNote: FeatureFlag<String>,
  ): Set<FeatureFlag<*>> = setOf(
    reviewEnabled,
    reviewPromptForce,
    folderPickerInSettings,
    experimentalPlaybackPersistence,
    media3AudioOffload,
    kioskMode,
    supportDevelopment,
    supporterNote,
  )
}

@Qualifier
annotation class ReviewEnabledFeatureFlagQualifier

@Qualifier
annotation class ReviewPromptForceFeatureFlagQualifier

@Qualifier
annotation class UserAgentFeatureFlagQualifier

@Qualifier
annotation class FolderPickerInSettingsFeatureFlagQualifier

@Qualifier
annotation class ExperimentalPlaybackPersistenceQualifier

@Qualifier
annotation class Media3AudioOffloadFeatureFlagQualifier

@Qualifier
annotation class KioskModeFeatureFlagQualifier

@Qualifier
annotation class SupportDevelopmentFeatureFlagQualifier

@Qualifier
annotation class SupporterNoteFeatureFlagQualifier
