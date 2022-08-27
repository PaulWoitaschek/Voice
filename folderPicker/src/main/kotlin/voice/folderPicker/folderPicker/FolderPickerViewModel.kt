package voice.folderPicker.folderPicker

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import voice.common.navigation.Destination
import voice.common.navigation.Navigator
import voice.data.folders.AudiobookFolders
import voice.data.folders.FolderType
import javax.inject.Inject

class FolderPickerViewModel
@Inject constructor(
  private val audiobookFolders: AudiobookFolders,
  private val navigator: Navigator,
) {

  @Composable
  fun viewState(): FolderPickerViewState {
    val folders: List<FolderPickerViewState.Item> by remember {
      items()
    }.collectAsState(initial = emptyList())
    return FolderPickerViewState(folders)
  }

  private fun items(): Flow<List<FolderPickerViewState.Item>> {
    return audiobookFolders.all().map { folders ->
      withContext(Dispatchers.IO) {
        folders.flatMap { (folderType, folders) ->
          folders.map { documentFile ->
            FolderPickerViewState.Item(
              name = documentFile.displayName(),
              id = documentFile.uri,
              folderType = folderType,
            )
          }
        }.sortedDescending()
      }
    }
  }

  internal fun add(uri: Uri, type: FileTypeSelection) {
    when (type) {
      FileTypeSelection.File -> {
        audiobookFolders.add(uri, FolderType.SingleFile)
      }
      FileTypeSelection.Folder -> {
        navigator.goTo(Destination.SelectFolderType(uri))
      }
    }
  }

  fun removeFolder(item: FolderPickerViewState.Item) {
    audiobookFolders.remove(item.id, item.folderType)
  }
}

private fun DocumentFile.displayName(): String {
  val name = name
  return if (name == null) {
    uri.pathSegments.lastOrNull()
      ?.dropWhile { it != ':' }
      ?.removePrefix(":")
      ?.takeUnless { it.isBlank() }
      ?: uri.toString()
  } else {
    name.substringBeforeLast(".")
      .takeUnless { it.isEmpty() }
      ?: name
  }
}
