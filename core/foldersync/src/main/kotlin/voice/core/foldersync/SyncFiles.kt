package voice.core.foldersync

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import dev.zacsweers.metro.Inject
import java.io.IOException
import voice.core.logging.api.Logger

/**
 * The sync files of every device, in a `.voice` folder inside a library folder. The folder's name
 * starts with a dot, so the scanner skips it.
 */
@Inject
internal class SyncFiles(private val context: Context) {

  /** Every device's file, by its name, or null when the folder can't be read. */
  fun read(folder: Uri): Map<String, SyncFile>? = fromProvider {
    val voiceFolder =
      children(folder, DocumentsContract.getTreeDocumentId(folder)).find { it.name == FOLDER_NAME }
        ?: return emptyMap()
    children(folder, voiceFolder.id)
      .filter { it.name.startsWith("sync-") && it.name.endsWith(".json") }
      .mapNotNull { child ->
        val uri = DocumentsContract.buildDocumentUriUsingTree(folder, child.id)
        context.contentResolver
          .openInputStream(uri)
          ?.use { it.readBytes().decodeToString() }
          ?.let(::parseSyncFile)
          ?.let { child.name to it }
      }
      .toMap()
  }

  fun canWrite(folder: Uri): Boolean =
    context.contentResolver.persistedUriPermissions.any {
      it.uri == folder && it.isWritePermission
    }

  fun write(
    folder: Uri,
    name: String,
    file: SyncFile,
  ) {
    val _ = fromProvider {
      val voiceFolder =
        childUri(
          folder,
          DocumentsContract.getTreeDocumentId(folder),
          FOLDER_NAME,
          Document.MIME_TYPE_DIR,
        )
      val uri =
        childUri(folder, DocumentsContract.getDocumentId(voiceFolder), name, "application/json")
      val stream =
        context.contentResolver.openOutputStream(uri, "wt")
          ?: throw IOException("Can't write a sync file")
      stream.use { it.write(file.encode().toByteArray()) }
    }
  }

  fun delete(
    folder: Uri,
    name: String,
  ) {
    val _ = fromProvider {
      val voiceFolder =
        children(folder, DocumentsContract.getTreeDocumentId(folder)).find {
          it.name == FOLDER_NAME
        } ?: return
      // the folder stays, as deleting it would also delete a file a sync tool puts in at that
      // moment
      val file = children(folder, voiceFolder.id).find { it.name == name } ?: return
      val uri = DocumentsContract.buildDocumentUriUsingTree(folder, file.id)
      if (!DocumentsContract.deleteDocument(context.contentResolver, uri)) {
        throw IOException("Can't delete a sync file")
      }
    }
  }

  private fun childUri(
    folder: Uri,
    parentId: String,
    name: String,
    mimeType: String,
  ): Uri {
    val existing = children(folder, parentId).find { it.name == name }
    if (existing != null) {
      return DocumentsContract.buildDocumentUriUsingTree(folder, existing.id)
    }
    val parent = DocumentsContract.buildDocumentUriUsingTree(folder, parentId)
    return DocumentsContract.createDocument(context.contentResolver, parent, mimeType, name)
      ?: throw IOException("Can't create a $mimeType document")
  }

  private fun children(
    folder: Uri,
    parentId: String,
  ): List<Child> {
    val uri = DocumentsContract.buildChildDocumentsUriUsingTree(folder, parentId)
    val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME)
    return context.contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
      buildList {
        while (cursor.moveToNext()) {
          val id = cursor.getString(0) ?: continue
          val name = cursor.getString(1) ?: continue
          add(Child(id, name))
        }
      }
    } ?: emptyList()
  }

  // the storage provider can send any exception across the binder, like a SecurityException when
  // the listener took away the access, or a NullPointerException from a bug of its own
  private inline fun <T> fromProvider(action: () -> T): T? =
    try {
      action()
    } catch (e: Exception) {
      Logger.w(e, "Could not sync through a folder")
      null
    }

  private data class Child(
    val id: String,
    val name: String,
  )
}

private const val FOLDER_NAME = ".voice"
