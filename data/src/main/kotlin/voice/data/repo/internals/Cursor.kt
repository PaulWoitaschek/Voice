package voice.data.repo.internals

import android.database.Cursor
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull

fun Cursor.getLong(columnName: String) = getLong(getColumnIndexOrThrow(columnName))
fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))
fun Cursor.getFloat(columnName: String) = getFloat(getColumnIndexOrThrow(columnName))
fun Cursor.getInt(columnName: String) = getInt(getColumnIndexOrThrow(columnName))
fun Cursor.getIntOrNull(columnName: String) = getIntOrNull(getColumnIndexOrThrow(columnName))
fun Cursor.getStringOrNull(columnName: String) = getStringOrNull(getColumnIndexOrThrow(columnName))
