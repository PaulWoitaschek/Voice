package voice.features.folderPicker.addcontent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.navigation.Origin

@ContributesTo(AppScope::class)
interface AddContentGraph {
  val viewModelFactory: AddContentViewModel.Factory
}

@BindingContainer
@ContributesTo(AppScope::class)
object AddContentProvider {

  @Provides
  @IntoSet
  fun addContentNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.AddContent> { key ->
    NavEntry(key) {
      AddContent(origin = key.origin)
    }
  }
}

@Composable
fun AddContent(origin: Origin) {
  val viewModel = retain(origin.name) {
    rootGraphAs<AddContentGraph>().viewModelFactory.create(origin)
  }
  SelectFolder(
    onBack = {
      viewModel.back()
    },
    origin = origin,
    onAdd = { folderType, uri ->
      viewModel.add(uri, folderType)
    },
    onFolderPickerCancel = viewModel::onFolderPickerCancel,
    onConnectAudiobookshelf = if (viewModel.canConnectAudiobookshelf) viewModel::connectAudiobookshelf else null,
  )
}
