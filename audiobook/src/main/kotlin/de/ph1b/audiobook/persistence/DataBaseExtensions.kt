package de.ph1b.audiobook.persistence

import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

fun Cursor.string(columnName: String): String {
    return stringNullable(columnName)!!
}

fun Cursor.stringNullable(columnName: String): String? {
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

inline fun SQLiteDatabase.asTransaction(func: SQLiteDatabase.() -> Unit) {
    beginTransaction()
    try {
        func()
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}

inline fun SharedPreferences.edit(func: SharedPreferences.Editor.() -> Unit) {
    val editor = this.edit()
    editor.func()
    editor.apply()
}

fun SharedPreferences.Editor.setString(pair: Pair<String, String>) =
        putString(pair.first, pair.second)

fun SharedPreferences.Editor.setLong(pair: Pair<String, Long>) =
        putLong(pair.first, pair.second)

fun SharedPreferences.Editor.setInt(pair: Pair<String, Int>) =
        putInt(pair.first, pair.second)

fun SharedPreferences.Editor.setStringSet(pair: Pair<String, Set<String>>) =
        putStringSet(pair.first, pair.second)