package voice.core.data.folders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import voice.core.analytics.api.Analytics
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.CachedDocumentFileFactory
import voice.core.logging.api.Logger

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
public class AudiobookFoldersImpl
internal constructor(
  private val rootAudioBookFoldersStore: RootAudiobookFoldersStore,
  private val singleFolderAudiobookFoldersStore: SingleFolderAudiobookFoldersStore,
  private val singleFileAudiobookFoldersStore: SingleFileAudiobookFoldersStore,
  private val authorAudiobookFoldersStore: AuthorAudiobookFoldersStore,
  private val context: Context,
  private val cachedDocumentFileFactory: CachedDocumentFileFactory,
  private val analytics: Analytics,
  private val persistedUriPermissions: PersistedUriPermissions,
) : AudiobookFolders {

  private val scope = MainScope()

  // allowing access to a folder that's already stored again leaves the stores unchanged, so they don't emit
  private val grantsChanged = MutableStateFlow(0)

  private val storedFolders: Flow<StoredFolders> = combine(
    FolderType.entries.map { folderType -> dataStore(folderType).data.map { folderType to it } },
  ) { it.toMap() }
    .combine(grantsChanged) { stored, _ ->
      StoredFolders(stored = stored, withAccess = persistedUriPermissions.persistedUris())
    }
    // reading the access asks the system, which the library would otherwise do on the main thread at start
    .flowOn(Dispatchers.IO)

  public override fun all(): Flow<Map<FolderType, List<DocumentFileWithUri>>> {
    return storedFolders.map { folders ->
      folders.stored.mapValues { (folderType, uris) ->
        uris
          .filter { it in folders.withAccess }
          .map { uri ->
            DocumentFileWithUri(
              documentFile = uri.toDocumentFile(folderType),
              uri = uri,
            )
          }
      }
    }
  }

  public override fun withoutAccess(): Flow<Map<FolderType, List<Uri>>> {
    return storedFolders.map { folders ->
      folders.stored.mapValues { (_, uris) ->
        uris.filter { it !in folders.withAccess }
      }
    }
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

  public override fun add(
    uri: Uri,
    type: FolderType,
  ) {
    analytics.event("add_folder", mapOf("type" to type.name))
    takePermission(uri, type)
    grantsChanged.update { it + 1 }
    scope.launch {
      dataStore(type).updateData { it + uri }
      // a folder has one type, so adding it again with another type moves it. It's added first so a scan in
      // between never misses its books.
      FolderType.entries
        .filter { it != type }
        .forEach { otherType ->
          dataStore(otherType).updateData { it - uri }
        }
    }
  }

  // one at a time, as taking both takes nothing when the picker only granted reading
  private fun takePermission(
    uri: Uri,
    type: FolderType,
  ) {
    val resolver = context.contentResolver
    try {
      resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
      Logger.w("Could not take the uri permission for a $type folder")
      return
    }
    try {
      resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    } catch (_: SecurityException) {
      Logger.w("Could only take the read permission for a $type folder")
    }
  }

  public override fun remove(
    uri: Uri,
    folderType: FolderType,
  ) {
    analytics.event("remove_folder", mapOf("type" to folderType.name))
    try {
      context.contentResolver.releasePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
    } catch (_: SecurityException) {
      Logger.v("Could not release the uri permission for a $folderType folder")
    }
    scope.launch {
      dataStore(folderType).updateData { folders ->
        folders - uri
      }
    }
  }

  private fun dataStore(type: FolderType): DataStore<Set<Uri>> {
    return when (type) {
      FolderType.SingleFile -> singleFileAudiobookFoldersStore
      FolderType.SingleFolder -> singleFolderAudiobookFoldersStore
      FolderType.Root -> rootAudioBookFoldersStore
      FolderType.Author -> authorAudiobookFoldersStore
    }
  }

  public override suspend fun hasAnyFolders(): Boolean {
    // folders without access count too, as the library asks to allow access to them again
    return FolderType.entries.any {
      dataStore(it).data.first().isNotEmpty()
    }
  }
}

private class StoredFolders(
  val stored: Map<FolderType, Set<Uri>>,
  val withAccess: Set<Uri>,
)
