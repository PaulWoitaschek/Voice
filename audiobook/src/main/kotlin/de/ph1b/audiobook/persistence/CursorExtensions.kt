package de.ph1b.audiobook.persistence

import android.database.Cursor

fun Cursor.string(columnName: String): String {
    return stringNullable(columnName)!!
}

fun Cursor.stringNullable(columnName: String) : String?{
    return getString(getColumnIndexOrThrow(columnName))
}

fun Cursor.float(columnName: String): Float {
    return getFloat(getColumnIndexOrThrow(columnName))
}

fun Cursor.long(columnName: String): Long {
    return getLong(getColumnIndexOrThrow(columnName))
}

fun Cursor.int(columnName: String): Int {
    return getInt(getColumnIndexOrThrow(columnName))
}