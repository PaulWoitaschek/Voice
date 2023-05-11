package voice.documentfile

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.database.getStringOrNull

class CachedDocumentFile
internal constructor(
  private val fileSystem: CachedDocumentFileSystem,
  private val uri: Uri,
  private val preFilledContent: FileContents? = null,
) {

  val children: List<CachedDocumentFile> by lazy {
    if (isDirectory) {
      parseContents(uri, fileSystem)
    } else {
      emptyList()
    }
  }

  private val content: FileContents? by lazy {
    preFilledContent ?: fileSystem.context.contentResolver.query(uri, FileContents.columns, null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        FileContents.readFrom(cursor)
      } else {
        null
      }
    }
  }

  val name: String? by lazy { content?.name }
  val isDirectory: Boolean by lazy { content?.isDirectory ?: false }
  val isFile: Boolean by lazy { content?.isFile ?: false }
  val length: Long by lazy { content?.length ?: 0L }
}

fun CachedDocumentFile.walk(): Sequence<CachedDocumentFile> = sequence {
  suspend fun SequenceScope<CachedDocumentFile>.walk(file: CachedDocumentFile) {
    yield(file)
    if (file.isDirectory) {
      file.children.forEach { walk(it) }
    }
  }
  walk(this@walk)
}

private fun parseContents(uri: Uri, fileSystem: CachedDocumentFileSystem): List<CachedDocumentFile> {
  val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
    uri,
    DocumentsContract.getDocumentId(uri),
  )
  return fileSystem.context.contentResolver.query(childrenUri, FileContents.columns, null, null, null)?.use { cursor ->
    val files = mutableListOf<CachedDocumentFile>()
    while (cursor.moveToNext()) {
      val documentId = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
      val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
      val contents = FileContents.readFrom(cursor)
      files += CachedDocumentFile(fileSystem, documentUri, contents)
    }
    files
  } ?: emptyList()
}
