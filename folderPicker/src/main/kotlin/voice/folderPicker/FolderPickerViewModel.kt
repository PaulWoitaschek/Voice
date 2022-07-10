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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import voice.common.pref.AudiobookFolders
import voice.logging.core.Logger
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier
annotation class ExplanationCardSeen

class FolderPickerViewModel
@Inject constructor(
  private val context: Context,
  @AudiobookFolders
  private val audiobookFolders: DataStore<List<@JvmSuppressWildcards Uri>>,
  @ExplanationCardSeen
  private val explanationCardSeen: DataStore<Boolean>,
) {

  private val scope = MainScope()

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
    val explanationCardSeen by explanationCardSeen.data.collectAsState(true)
    val explanationCard = if (explanationCardSeen) {
      null
    } else {
      """
        ${context.getString(R.string.audiobook_folder_card_text)}

        audiobooks/Harry Potter 1
        audiobooks/Harry Potter 2
      """.trimIndent()
    }
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
    try {
      context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (e: SecurityException) {
      Logger.w("Could not release uri permission for $uri")
    }
    scope.launch {
      audiobookFolders.updateData {
        it - uri
      }
    }
  }

  fun dismissExplanationCard() {
    scope.launch {
      explanationCardSeen.updateData { true }
    }
  }
}
