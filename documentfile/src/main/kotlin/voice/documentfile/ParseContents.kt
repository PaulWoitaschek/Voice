package voice.documentfile

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.database.getStringOrNull

internal fun parseContents(uri: Uri, context: Context): List<CachedDocumentFile> {
  val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
    uri,
    DocumentsContract.getDocumentId(uri),
  )
  return context.contentResolver.query(
    childrenUri,
    FileContents.columns,
    null,
    null,
    null,
  )?.use { cursor ->
    val files = mutableListOf<CachedDocumentFile>()
    while (cursor.moveToNext()) {
      val documentId = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
      val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
      val contents = FileContents.readFrom(cursor)
      files += RealCachedDocumentFile(context, documentUri, contents)
    }
    files
  } ?: emptyList()
}
