package voice.core.data.repo.internals

import android.database.Cursor
import androidx.core.database.getStringOrNull

internal fun Cursor.getLong(columnName: String) = getLong(getColumnIndexOrThrow(columnName))

internal fun Cursor.getString(columnName: String): String =
  getString(getColumnIndexOrThrow(columnName))

internal fun Cursor.getStringOrNull(columnName: String) =
  getStringOrNull(getColumnIndexOrThrow(columnName))

internal inline fun <T> Cursor.mapRows(mapper: Cursor.() -> T): List<T> = use {
  moveToPosition(-1)
  val list = ArrayList<T>(count)
  while (moveToNext()) {
    list.add(mapper())
  }
  list
}
