package voice.folderPicker

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.documentfile.provider.DocumentFile
import de.ph1b.audiobook.common.pref.AudiobookFolders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FolderPickerViewModel
@Inject constructor(
  private val context: Context,
  @AudiobookFolders
  private val audiobookFolders: DataStore<List<@JvmSuppressWildcards Uri>>
) {

  private val scope = MainScope()
  private val explanationCardSeenFlow = MutableStateFlow(false)

  @Composable
  fun viewState(): FolderPickerViewState {
    val folders by remember {
      audiobookFolders.data
        .map { folders ->
          withContext(Dispatchers.IO) {
            folders.map { uri ->
              val documentFile = DocumentFile.fromTreeUri(context, uri)
              FolderPickerViewState.Item(
                name = documentFile?.name ?: "",
                id = uri
              )
            }
          }
        }
    }.collectAsState(initial = emptyList())
    val explanationCardSeen by explanationCardSeenFlow.collectAsState()
    val explanationCard = if(explanationCardSeen) null else "Hey Cats"
    return FolderPickerViewState(explanationCard = explanationCard, folders)
  }

  fun addFolder(uri: Uri) {
    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    scope.launch {
      audiobookFolders.updateData {
        it + uri
      }
    }
  }

  fun removeFolder(uri: Uri) {
    context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    scope.launch {
      audiobookFolders.updateData {
        it - uri
      }
    }
  }

  fun dismissExplanationCard(){
    explanationCardSeenFlow.value = true
  }

  fun destroy() {
    scope.cancel()
  }
}
