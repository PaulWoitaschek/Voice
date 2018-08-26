package de.ph1b.audiobook.data.repo.internals

import android.database.Cursor
import androidx.core.database.getStringOrNull

fun Cursor.getLong(columnName: String) = getLong(getColumnIndexOrThrow(columnName))
fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))
fun Cursor.getFloat(columnName: String) = getFloat(getColumnIndexOrThrow(columnName))
fun Cursor.getInt(columnName: String) = getInt(getColumnIndexOrThrow(columnName))
fun Cursor.getStringOrNull(columnName: String) = getStringOrNull(getColumnIndexOrThrow(columnName))
