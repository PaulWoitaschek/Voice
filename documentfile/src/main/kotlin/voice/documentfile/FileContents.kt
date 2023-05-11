package voice.documentfile

import android.database.Cursor
import android.provider.DocumentsContract
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull

internal data class FileContents(
  val name: String?,
  val isFile: Boolean,
  val isDirectory: Boolean,
  val length: Long,
) {

  companion object {
    val columns = arrayOf(
      DocumentsContract.Document.COLUMN_DOCUMENT_ID,
      DocumentsContract.Document.COLUMN_MIME_TYPE,
      DocumentsContract.Document.COLUMN_DISPLAY_NAME,
      DocumentsContract.Document.COLUMN_SIZE,
    )

    fun readFrom(cursor: Cursor): FileContents {
      val mimeType = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_MIME_TYPE)
      return FileContents(
        name = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
        isFile = mimeType != null && mimeType != DocumentsContract.Document.MIME_TYPE_DIR,
        isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR,
        length = cursor.getLongOrNull(DocumentsContract.Document.COLUMN_SIZE) ?: 0L,
      )
    }
  }
}

private fun Cursor.getStringOrNull(columnName: String): String? = getStringOrNull(getColumnIndexOrThrow(columnName))
private fun Cursor.getLongOrNull(columnName: String): Long? = getLongOrNull(getColumnIndexOrThrow(columnName))
