package voice.folderPicker.addcontent

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.common.compose.VoiceTheme
import voice.common.compose.rememberScoped
import voice.common.navigation.Destination
import voice.common.navigation.NavEntryProvider
import voice.common.rootGraphAs

@ContributesTo(AppScope::class)
interface AddContentGraph {
  val viewModelFactory: AddContentViewModel.Factory
}

@ContributesTo(AppScope::class)
interface AddContentProvider {

  @Provides
  @IntoSet
  fun navEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.AddContent> { key, backStack ->
    NavEntry(key) {
      AddContent(mode = key.mode)
    }
  }
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
