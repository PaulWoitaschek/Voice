package voice.documentfile

import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull

class CachedDocumentFile
internal constructor(
  private val fileSystem: CachedDocumentFileSystem,
  private val uri: Uri,
  private val props: Properties? = null,
) {

  val children: List<CachedDocumentFile> by lazy {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
      uri,
      DocumentsContract.getDocumentId(uri),
    )
    fileSystem.context.contentResolver.query(
      childrenUri,
      arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_SIZE,
      ),
      null, null, null,
    )?.use { cursor ->
      val res = mutableListOf<CachedDocumentFile>()
      while (cursor.moveToNext()) {
        val documentId = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(
          uri,
          documentId,
        )

        val mimeType = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_MIME_TYPE)
        val p = Properties(
          name = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
          isFile = mimeType != null && mimeType != DocumentsContract.Document.MIME_TYPE_DIR,
          isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR,
          length = cursor.getLongOrNull(DocumentsContract.Document.COLUMN_SIZE) ?: 0L,
        )
        res += CachedDocumentFile(fileSystem, documentUri, p)
      }
      res
    } ?: emptyList()
  }

  private val properties: Properties? by lazy {
    props ?: fileSystem.context.contentResolver.query(
      uri,
      arrayOf(
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_SIZE,
      ),
      null,
      null,
      null,
    )?.use { cursor ->
      if (cursor.moveToFirst()) {
        val mimeType = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_MIME_TYPE)
        Properties(
          name = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
          isFile = mimeType != null && mimeType != DocumentsContract.Document.MIME_TYPE_DIR,
          isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR,
          length = cursor.getLongOrNull(DocumentsContract.Document.COLUMN_SIZE) ?: 0L,
        )
      } else {
        null
      }
    }
  }

  val name: String? by lazy { properties?.name }
  val isDirectory: Boolean by lazy { properties?.isDirectory ?: false }
  val isFile: Boolean by lazy { properties?.isFile ?: false }
  val length: Long by lazy { properties?.length ?: 0L }

  fun walk(): Sequence<CachedDocumentFile> = sequence {
    suspend fun SequenceScope<CachedDocumentFile>.walk(file: CachedDocumentFile) {
      yield(file)
      if (file.isDirectory) {
        file.children.forEach { walk(it) }
      }
    }
    walk(this@CachedDocumentFile)
  }
}

private fun Cursor.getStringOrNull(columnName: String): String? = getStringOrNull(getColumnIndexOrThrow(columnName))
private fun Cursor.getLongOrNull(columnName: String): Long? = getLongOrNull(getColumnIndexOrThrow(columnName))
