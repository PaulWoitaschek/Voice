package voice.features.folderPicker.addcontent

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.FeatureFlagFactory

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<FeatureFlag<*>>())
class FolderHelpSurveyUrlFeatureFlag(flag: FeatureFlag<String>) : FeatureFlag<String> by flag {

  @Inject
  constructor(
    factory: FeatureFlagFactory,
  ) : this(
    factory.string(
      key = "folder_help_survey_url",
      description =
        "The URL of a survey the help after closing the folder picker without a folder invites to. Empty hides the button.",
      defaultValue = "",
    ),
  )
}
