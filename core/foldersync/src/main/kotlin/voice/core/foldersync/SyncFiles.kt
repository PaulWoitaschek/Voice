package voice.core.foldersync

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import dev.zacsweers.metro.Inject
import java.io.IOException

/**
 * The sync files of every device, in a `.voice` folder inside a library folder. The folder's name starts with a dot,
 * so the scanner skips it.
 */
@Inject
internal class SyncFiles(private val context: Context) {

  fun read(
    folder: Uri,
    exceptName: String,
  ): List<SyncFile> {
    val voiceFolder = children(folder, DocumentsContract.getTreeDocumentId(folder))
      .find { it.name == FOLDER_NAME } ?: return emptyList()
    return children(folder, voiceFolder.id)
      .filter { it.name.startsWith("sync-") && it.name.endsWith(".json") && it.name != exceptName }
      .mapNotNull { child ->
        val uri = DocumentsContract.buildDocumentUriUsingTree(folder, child.id)
        context.contentResolver.openInputStream(uri)
          ?.use { it.readBytes().decodeToString() }
          ?.let(::parseSyncFile)
      }
  }

  fun write(
    folder: Uri,
    name: String,
    file: SyncFile,
  ) {
    val voiceFolder = childUri(folder, DocumentsContract.getTreeDocumentId(folder), FOLDER_NAME, Document.MIME_TYPE_DIR)
    val uri = childUri(folder, DocumentsContract.getDocumentId(voiceFolder), name, "application/json")
    val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Can't write to $uri")
    stream.use { it.write(file.encode().toByteArray()) }
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
      ?: throw IOException("Can't create $name in $parent")
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

  private data class Child(
    val id: String,
    val name: String,
  )
}

private const val FOLDER_NAME = ".voice"
