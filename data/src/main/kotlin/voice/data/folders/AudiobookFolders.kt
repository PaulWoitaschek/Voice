package voice.data.folders

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import voice.common.pref.RootAudiobookFolders
import voice.common.pref.SingleFileAudiobookFolders
import voice.common.pref.SingleFolderAudiobookFolders
import voice.logging.core.Logger
import javax.inject.Inject

class AudiobookFolders
@Inject constructor(
  @RootAudiobookFolders
  private val rootAudioBookFolders: DataStore<List<@JvmSuppressWildcards Uri>>,
  @SingleFolderAudiobookFolders
  private val singleFolderAudiobookFolders: DataStore<List<@JvmSuppressWildcards Uri>>,
  @SingleFileAudiobookFolders
  private val singleFileAudiobookFolders: DataStore<List<@JvmSuppressWildcards Uri>>,
  private val application: Application,
) {

  private val scope = MainScope()

  fun all(): Flow<Map<FolderType, List<DocumentFile>>> {
    val flows = FolderType.values()
      .map { folderType ->
        dataStore(folderType).data.map { uris ->
          val documentFiles = uris.mapNotNull { uri ->
            uri.toDocumentFile(folderType)
          }
          folderType to documentFiles
        }
      }
    return combine(flows) { it.toMap() }
  }

  private fun Uri.toDocumentFile(
    folderType: FolderType,
  ): DocumentFile? {
    return when (folderType) {
      FolderType.SingleFile -> {
        DocumentFile.fromSingleUri(application, this)
      }
      FolderType.SingleFolder,
      FolderType.Root,
      -> DocumentFile.fromTreeUri(application, this)
    }
  }

  fun add(uri: Uri, type: FolderType) {
    application.contentResolver.takePersistableUriPermission(
      uri,
      Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
    )
    scope.launch {
      dataStore(type).updateData {
        (it + uri).distinct()
      }
    }
  }

  fun remove(uri: Uri, folderType: FolderType) {
    try {
      application.contentResolver.releasePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
    } catch (e: SecurityException) {
      Logger.w("Could not release uri permission for $uri")
    }
    scope.launch {
      dataStore(folderType).updateData { folders ->
        val documentFiles = folders.mapNotNull { uri ->
          uri.toDocumentFile(folderType)
        }
        val uriDocumentFile = uri.toDocumentFile(folderType)
        if (uriDocumentFile == null) {
          documentFiles.map { it.uri }
        } else {
          documentFiles.map { it.uri } - uriDocumentFile.uri
        }
      }
    }
  }

  private fun dataStore(type: FolderType) = when (type) {
    FolderType.SingleFile -> singleFileAudiobookFolders
    FolderType.SingleFolder -> singleFolderAudiobookFolders
    FolderType.Root -> rootAudioBookFolders
  }
}
