package voice.folderPicker.addcontent

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope
import voice.common.compose.VoiceTheme
import voice.common.compose.rememberScoped
import voice.common.navigation.Destination
import voice.common.rootComponentAs

@ContributesTo(AppScope::class)
interface AddContentComponent {
  val viewModelFactory: AddContentViewModel.Factory
}

@Composable
fun AddContent(mode: Destination.AddContent.Mode) {
  val viewModel = rememberScoped(mode.name) {
    rootComponentAs<AddContentComponent>().viewModelFactory.create(mode)
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
