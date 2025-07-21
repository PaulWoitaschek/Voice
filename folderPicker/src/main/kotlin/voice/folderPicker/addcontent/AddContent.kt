package voice.folderPicker.addcontent

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import voice.common.compose.VoiceTheme
import voice.common.compose.rememberScoped
import voice.common.navigation.Destination
import voice.common.rootGraphAs

@ContributesTo(AppScope::class)
interface AddContentGraph {
  val viewModelFactory: AddContentViewModel.Factory
}

@Composable
fun AddContent(mode: Destination.AddContent.Mode) {
  val viewModel = rememberScoped(mode.name) {
    rootGraphAs<AddContentGraph>().viewModelFactory.create(mode)
  }
  SelectFolder(
    onBack = {
      viewModel.back()
    },
    mode = mode,
    onAdd = { folderType, uri ->
      viewModel.add(uri, folderType)
    },
  )
}

@Composable
@Preview
private fun AddContentPreview() {
  VoiceTheme {
    AddContent(Destination.AddContent.Mode.Default)
  }
}
