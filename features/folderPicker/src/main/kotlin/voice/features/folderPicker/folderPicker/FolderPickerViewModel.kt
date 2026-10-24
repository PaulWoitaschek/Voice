package voice.features.folderPicker.folderPicker

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.FolderType
import voice.core.documentfile.nameFromPath
import voice.core.documentfile.nameWithoutExtension
import voice.core.featureflag.FeatureFlag
import voice.core.kioskmode.KioskModeFeatureFlag
import voice.core.scanner.MediaScanTrigger
import voice.navigation.Destination
import voice.navigation.Navigator
import voice.navigation.Origin

@Inject
class FolderPickerViewModel(
  private val audiobookFolders: AudiobookFolders,
  private val navigator: Navigator,
  private val mediaScanTrigger: MediaScanTrigger,
  private val kioskModeFeatureFlag: KioskModeFeatureFlag,
) {

  @Composable
  fun viewState(): FolderPickerViewState {
    val kioskMode = remember { kioskModeFeatureFlag.get() }
    if (kioskMode) {
      return FolderPickerViewState(
        items = kioskModeItems,
        showActions = false,
      )
    }

    val folders: List<FolderPickerViewState.Item>? by remember {
      items()
    }.collectAsState(initial = null)
    return FolderPickerViewState(
      items = folders.orEmpty(),
      // keeps the empty state from flashing up while the folders are read
      loading = folders == null,
    )
  }

  private fun items(): Flow<List<FolderPickerViewState.Item>> {
    return combine(audiobookFolders.all(), audiobookFolders.withoutAccess()) { folders, foldersWithoutAccess ->
      withContext(Dispatchers.IO) {
        val withAccess = folders.flatMap { (folderType, folders) ->
          folders.map { (documentFile, uri) ->
            FolderPickerViewState.Item(
              name = documentFile.nameWithoutExtension(),
              id = uri,
              folderType = folderType,
            )
          }
        }
        // right after access is allowed again, the two flows can disagree for a moment
        val uris = withAccess.map { it.id }.toSet()
        val withoutAccess = foldersWithoutAccess.flatMap { (folderType, uris) ->
          uris.map { uri ->
            FolderPickerViewState.Item(
              // the folder can't be asked for its name without access
              name = uri.nameFromPath(),
              id = uri,
              folderType = folderType,
              hasAccess = false,
            )
          }
        }.filter { it.id !in uris }
        (withAccess + withoutAccess).sortedWith(compareBy<FolderPickerViewState.Item> { it.hasAccess }.then(reverseOrder()))
      }
    }
  }

  internal fun onCloseClick() {
    navigator.goBack()
  }

  internal fun add() {
    navigator.goTo(
      Destination.AddContent(
        Origin.Default,
      ),
    )
  }

  internal fun changeType(item: FolderPickerViewState.Item) {
    navigator.goTo(
      Destination.SelectFolderType(
        uri = item.id,
        origin = Origin.Default,
        currentType = item.folderType,
      ),
    )
  }

  fun removeFolder(item: FolderPickerViewState.Item) {
    audiobookFolders.remove(item.id, item.folderType)
  }

  internal fun onAccessAllowed(
    uri: Uri,
    folderType: FolderType,
    pickedUri: Uri,
  ) {
    if (pickedUri == uri) {
      audiobookFolders.add(uri, folderType)
      mediaScanTrigger.scan(restartIfScanning = true)
      return
    }
    audiobookFolders.remove(uri, folderType)
    if (folderType == FolderType.SingleFile) {
      audiobookFolders.add(pickedUri, folderType)
      mediaScanTrigger.scan(restartIfScanning = true)
    } else {
      // another folder can hold its books differently, so its type is chosen again
      navigator.goTo(Destination.SelectFolderType(uri = pickedUri, origin = Origin.Default))
    }
  }

  private companion object {
    // made up folders, each with its own uri, as the uri tells them apart
    val kioskModeItems = listOf(
      FolderPickerViewState.Item(
        name = "Audiobooks",
        id = "kiosk:audiobooks".toUri(),
        folderType = FolderType.Root,
      ),
      FolderPickerViewState.Item(
        name = "Sci-Fi",
        id = "kiosk:sci-fi".toUri(),
        folderType = FolderType.SingleFolder,
      ),
      FolderPickerViewState.Item(
        name = "Non-Fiction",
        id = "kiosk:non-fiction".toUri(),
        folderType = FolderType.SingleFolder,
      ),
    )
  }
}
