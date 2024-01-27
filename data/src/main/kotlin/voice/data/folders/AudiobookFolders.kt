package voice.data.folders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.datastore.core.DataStore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import voice.common.pref.AuthorAudiobookFolders
import voice.common.pref.RootAudiobookFolders
import voice.common.pref.SingleFileAudiobookFolders
import voice.common.pref.SingleFolderAudiobookFolders
import voice.documentfile.CachedDocumentFile
import voice.documentfile.CachedDocumentFileFactory
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
  @AuthorAudiobookFolders
  private val authorAudiobookFolders: DataStore<List<@JvmSuppressWildcards Uri>>,
  private val context: Context,
  private val cachedDocumentFileFactory: CachedDocumentFileFactory,
) {

  private val scope = MainScope()

  fun all(): Flow<Map<FolderType, List<DocumentFileWithUri>>> {
    val flows = FolderType.entries
      .map { folderType ->
        dataStore(folderType).data.map { uris ->
          val documentFiles = uris.map { uri ->
            DocumentFileWithUri(uri.toDocumentFile(folderType), uri)
          }
          folderType to documentFiles
        }
      }
    return combine(flows) { it.toMap() }
  }

  private fun Uri.toDocumentFile(folderType: FolderType): CachedDocumentFile {
    val uri = when (folderType) {
      FolderType.SingleFile -> this
      FolderType.SingleFolder,
      FolderType.Root,
      FolderType.Author,
      -> {
        DocumentsContract.buildDocumentUriUsingTree(
          this,
          DocumentsContract.getTreeDocumentId(this),
        )
      }
    }
    return cachedDocumentFileFactory.create(uri)
  }

  fun add(
    uri: Uri,
    type: FolderType,
  ) {
    context.contentResolver.takePersistableUriPermission(
      uri,
      Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
    )
    scope.launch {
      dataStore(type).updateData {
        (it + uri).distinct()
      }
    }
  }

  fun remove(
    uri: Uri,
    folderType: FolderType,
  ) {
    try {
      context.contentResolver.releasePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
    } catch (e: SecurityException) {
      Logger.w("Could not release uri permission for $uri")
    }
    scope.launch {
      dataStore(folderType).updateData { folders ->
        folders - uri
      }
    }
  }

  private fun dataStore(type: FolderType): DataStore<List<Uri>> {
    return when (type) {
      FolderType.SingleFile -> singleFileAudiobookFolders
      FolderType.SingleFolder -> singleFolderAudiobookFolders
      FolderType.Root -> rootAudioBookFolders
      FolderType.Author -> authorAudiobookFolders
    }
  }

  suspend fun hasAnyFolders(): Boolean {
    return FolderType.entries.any {
      dataStore(it).data.first().isNotEmpty()
    }
  }
}

data class DocumentFileWithUri(
  val documentFile: CachedDocumentFile,
  val uri: Uri,
)
