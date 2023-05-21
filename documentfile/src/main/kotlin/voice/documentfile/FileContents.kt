package voice.documentfile

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import voice.logging.core.Logger

internal data class FileContents(
  val name: String?,
  val isFile: Boolean,
  val isDirectory: Boolean,
  val length: Long,
  val lastModified: Long,
) {

  companion object {
    val columns = arrayOf(
      DocumentsContract.Document.COLUMN_DOCUMENT_ID,
      DocumentsContract.Document.COLUMN_MIME_TYPE,
      DocumentsContract.Document.COLUMN_DISPLAY_NAME,
      DocumentsContract.Document.COLUMN_SIZE,
      DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )

    fun readFrom(cursor: Cursor): FileContents {
      val mimeType = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_MIME_TYPE)
      return FileContents(
        name = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
        isFile = mimeType != null && mimeType != DocumentsContract.Document.MIME_TYPE_DIR,
        isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR,
        length = cursor.getLongOrNull(DocumentsContract.Document.COLUMN_SIZE) ?: 0L,
        lastModified = cursor.getLongOrNull(DocumentsContract.Document.COLUMN_LAST_MODIFIED) ?: 0L,
      )
    }

    fun query(context: Context, uri: Uri): FileContents? {
      return context.query(uri)?.use { cursor ->
        if (cursor.moveToFirst()) {
          readFrom(cursor)
        } else {
          null
        }
      }
    }
  }
}

private fun Context.query(uri: Uri): Cursor? {
  return try {
    contentResolver.query(
      uri,
      FileContents.columns,
      null,
      null,
      null,
    )
  } catch (e: SecurityException) {
    Logger.e(e, "Error while querying $uri")
    null
  }
}

private fun Cursor.getStringOrNull(columnName: String): String? = getStringOrNull(getColumnIndexOrThrow(columnName))
private fun Cursor.getLongOrNull(columnName: String): Long? = getLongOrNull(getColumnIndexOrThrow(columnName))
