package voice.features.folderPicker.addcontent

import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.ui.OnboardingScaffold
import voice.core.ui.OnboardingStep
import voice.core.ui.VoiceTheme
import voice.features.folderPicker.folderPicker.FileTypeSelection
import voice.navigation.Origin
import voice.core.strings.R as StringsR

@Composable
internal fun SelectFolder(
  onBack: () -> Unit,
  onAdd: (FileTypeSelection, Uri) -> Unit,
  /** Null while connecting a server isn't available. */
  onConnectAudiobookshelf: (() -> Unit)?,
  onFolderPickerCancel: () -> Unit,
  origin: Origin,
  modifier: Modifier = Modifier,
) {
  var help by rememberSaveable { mutableStateOf<FolderHelp?>(null) }
  OnboardingScaffold(
    modifier = modifier,
    // outside of the onboarding there are no steps to count
    step = when (origin) {
      Origin.Default -> null
      Origin.Onboarding -> OnboardingStep.AddContent
    },
    onBack = onBack,
    title = stringResource(
      when (origin) {
        Origin.Default -> StringsR.string.folder_add_title_default
        Origin.Onboarding -> StringsR.string.folder_add_title_onboarding
      },
    ),
    subtitle = stringResource(StringsR.string.folder_add_subtitle),
    hero = { clock ->
      FolderHero(clock = clock, modifier = Modifier.fillMaxSize())
    },
    details = {
      help?.let {
        Spacer(Modifier.height(24.dp))
        FolderHelpCard(it)
      }
    },
    actions = {
      ContentTypeChoices(
        onAdd = onAdd,
        onFolderPickerCancel = {
          help = FolderHelp.PickerClosed
          onFolderPickerCancel()
        },
        onNoPicker = { help = FolderHelp.NoPicker },
        onConnectAudiobookshelf = onConnectAudiobookshelf,
      )
    },
  )
}

@Composable
@Preview
private fun SelectFolderPreview() {
  VoiceTheme {
    SelectFolder(
      onBack = {},
      onAdd = { _, _ -> },
      onConnectAudiobookshelf = {},
      onFolderPickerCancel = {},
      origin = Origin.Onboarding,
    )
  }
}
