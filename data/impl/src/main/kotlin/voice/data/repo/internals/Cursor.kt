package voice.data.repo.internals

import android.database.Cursor
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull

internal fun Cursor.getLong(columnName: String) = getLong(getColumnIndexOrThrow(columnName))
internal fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))
internal fun Cursor.getFloat(columnName: String) = getFloat(getColumnIndexOrThrow(columnName))
internal fun Cursor.getInt(columnName: String) = getInt(getColumnIndexOrThrow(columnName))
internal fun Cursor.getIntOrNull(columnName: String) = getIntOrNull(getColumnIndexOrThrow(columnName))
internal fun Cursor.getStringOrNull(columnName: String) = getStringOrNull(getColumnIndexOrThrow(columnName))
