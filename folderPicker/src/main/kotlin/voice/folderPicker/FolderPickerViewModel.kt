package voice.folderPicker

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import voice.data.folders.AudiobookFolders
import voice.data.folders.FolderType
import javax.inject.Inject

class FolderPickerViewModel
@Inject constructor(
  private val audiobookFolders: AudiobookFolders,
) {

  @Composable
  fun viewState(): FolderPickerViewState {
    val folders: List<FolderPickerViewState.Item> by remember {
      audiobookFolders.all()
        .map { folders ->
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
    }.collectAsState(initial = emptyList())
    return FolderPickerViewState(folders)
  }

  fun add(uri: Uri, type: FolderType) {
    audiobookFolders.add(uri, type)
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
