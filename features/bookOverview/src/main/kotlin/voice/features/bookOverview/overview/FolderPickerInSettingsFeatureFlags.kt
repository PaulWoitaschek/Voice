package voice.features.bookOverview.overview

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
annotation class FolderPickerInSettingsFeatureFlagQualifier

@BindingContainer
@ContributesTo(AppScope::class)
object FolderPickerInSettingsFeatureFlags {

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
  @IntoSet
  fun folderPickerInSettingsFeatureFlagIntoSet(@FolderPickerInSettingsFeatureFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<*> =
    flag
}
