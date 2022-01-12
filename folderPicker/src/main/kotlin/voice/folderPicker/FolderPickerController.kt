package voice.folderPicker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.squareup.anvil.annotations.ContributesTo
import de.ph1b.audiobook.AppScope
import de.ph1b.audiobook.rootComponentAs
import voice.common.compose.ComposeController
import javax.inject.Inject


class FolderPickerController : ComposeController() {

  @Inject
  lateinit var viewModel: FolderPickerViewModel

  init {
    rootComponentAs<Component>().inject(this)
  }

  @Composable
  override fun Content() {
    val viewState = viewModel.viewState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
      if (uri != null) {
        viewModel.addFolder(uri)
      }
    }
    FolderPickerView(
      viewState = viewState,
      onAddClick = {
        launcher.launch(null)
      },
      onDeleteClick = {
        viewModel.removeFolder(it)
      },
      onDismissExplanationCardClick = {
        viewModel.dismissExplanationCard()
      },
      onCloseClick = {
        router.popController(this)
      }
    )
  }

  override fun onDestroy() {
    super.onDestroy()
    viewModel.destroy()
  }

  @ContributesTo(AppScope::class)
  interface Component {
    fun inject(target: FolderPickerController)
  }
}
