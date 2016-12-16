package de.ph1b.audiobook.persistence.internals

import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.util.*

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

inline fun <T> SQLiteDatabase.asTransaction(func: SQLiteDatabase.() -> T): T {
  beginTransaction()
  try {
    val result = func()
    setTransactionSuccessful()
    return result
  } finally {
    endTransaction()
  }
}

inline fun SharedPreferences.edit(func: SharedPreferences.Editor.() -> Unit) {
  val editor = this.edit()
  editor.func()
  editor.apply()
}

inline fun Cursor.moveToNextLoop(func: Cursor.() -> Unit) {
  try {
    while (moveToNext()) {
      func()
    }
  } finally {
    close()
  }
}

/** a function that iterates of the rows of a cursor and maps all using a supplied mapper function */
inline fun <T> Cursor.mapRows(mapper: Cursor.() -> T): List<T> {
  val list = ArrayList<T>(count)
  try {
    while (moveToNext()) {
      list.add(mapper())
    }
  } finally {
    close()
  }
  return list
}


fun SQLiteDatabase.simpleQuery(
  table: String,
  columns: Array<String>? = null,
  selection: String? = null,
  selectionArgs: Array<String>? = null,
  groupBy: String? = null,
  having: String? = null,
  orderBy: String? = null,
  limit: String? = null,
  distinct: Boolean = false): Cursor {
  return query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit)
}