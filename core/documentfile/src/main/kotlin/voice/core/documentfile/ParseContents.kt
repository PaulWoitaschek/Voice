package voice.core.documentfile

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.database.getStringOrNull
import voice.core.logging.api.Logger

internal fun parseContents(
  uri: Uri,
  context: Context,
): List<CachedDocumentFile> {
  return context.query(uri)?.use { cursor ->
    cursor.parseRows(uri, context)
  } ?: emptyList()
}

private fun Cursor.parseRows(
  uri: Uri,
  context: Context,
): List<CachedDocumentFile> {
  val files = mutableListOf<CachedDocumentFile>()
  while (moveToNext()) {
    val documentId = getStringOrNull(getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
    val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
    val contents = FileContents.readFrom(this)
    if (contents.name?.startsWith(".") == true) {
      Logger.v("Ignoring hidden file $contents")
      continue
    }
    files += RealCachedDocumentFile(context, documentUri, contents)
  }
  return files
}

private fun Context.query(uri: Uri): Cursor? {
  val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
    uri,
    DocumentsContract.getDocumentId(uri),
  )
  return try {
    contentResolver.query(
      childrenUri,
      FileContents.columns,
      null,
      null,
      null,
    )
  } catch (e: Exception) {
    Logger.w(e, "Can't parse contents for $uri")
    null
  }
}
