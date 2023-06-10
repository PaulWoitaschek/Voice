package voice.onboarding.addcontent

import android.net.Uri
import voice.common.navigation.Destination
import voice.common.navigation.Navigator
import voice.data.folders.AudiobookFolders
import voice.data.folders.FolderType
import voice.folderPicker.folderPicker.FileTypeSelection
import javax.inject.Inject

class OnboardingAddContentViewModel
@Inject constructor(
  private val audiobookFolders: AudiobookFolders,
  private val navigator: Navigator,
) {

  internal fun add(uri: Uri, type: FileTypeSelection) {
    when (type) {
      FileTypeSelection.File -> {
        audiobookFolders.add(uri, FolderType.SingleFile)
        navigator.goTo(Destination.OnboardingCompletion)
      }
      FileTypeSelection.Folder -> {
        navigator.goTo(
          Destination.SelectFolderType(
            uri = uri,
            mode = Destination.SelectFolderType.Mode.Onboarding,
          ),
        )
      }
    }
  }

  internal fun back() {
    navigator.goBack()
  }
}
