package voice.folderPicker.addcontent

import android.net.Uri
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import voice.common.navigation.Destination
import voice.common.navigation.Destination.AddContent.Mode
import voice.common.navigation.Destination.OnboardingCompletion
import voice.common.navigation.Destination.SelectFolderType
import voice.common.navigation.Navigator
import voice.data.folders.AudiobookFolders
import voice.data.folders.FolderType
import voice.folderPicker.folderPicker.FileTypeSelection
import com.kiwi.navigationcompose.typed.navigate as typedNavigate
import voice.common.navigation.Destination.SelectFolderType.Mode as SelectFolderTypeMode

@Inject
class AddContentViewModel(
  private val audiobookFolders: AudiobookFolders,
  private val navigator: Navigator,
  @Assisted
  private val mode: Mode,
) {

  internal fun add(
    uri: Uri,
    type: FileTypeSelection,
  ) {
    when (type) {
      FileTypeSelection.File -> {
        audiobookFolders.add(uri, FolderType.SingleFile)
        when (mode) {
          Mode.Default -> {
            navigator.execute { navController ->
              navController.typedNavigate(Destination.BookOverview) {
                popUpTo(0)
              }
            }
          }
          Mode.Onboarding -> {
            navigator.goTo(OnboardingCompletion)
          }
        }
      }
      FileTypeSelection.Folder -> {
        navigator.goTo(
          SelectFolderType(
            uri = uri,
            mode = when (mode) {
              Mode.Default -> SelectFolderTypeMode.Default
              Mode.Onboarding -> SelectFolderTypeMode.Onboarding
            },
          ),
        )
      }
    }
  }

  internal fun back() {
    navigator.goBack()
  }

  @AssistedFactory
  interface Factory {
    fun create(mode: Mode): AddContentViewModel
  }
}
