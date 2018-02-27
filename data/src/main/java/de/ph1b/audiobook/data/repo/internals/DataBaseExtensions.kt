package de.ph1b.audiobook.data.repo.internals

import android.annotation.SuppressLint
import android.content.ContentValues
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

fun Cursor.intNullable(columnName: String): Int? {
  val index = getColumnIndexOrThrow(columnName)
  return if (isNull(index)) null
  else getInt(index)
}

inline fun <T> SQLiteDatabase.asTransaction(func: SQLiteDatabase.() -> T): T {
  beginTransaction()
  return try {
    val result = func()
    setTransactionSuccessful()
    result
  } finally {
    endTransaction()
  }
}

inline fun SharedPreferences.edit(func: SharedPreferences.Editor.() -> Unit) {
  val editor = this.edit()
  editor.func()
  editor.apply()
}

inline fun Cursor.moveToNextLoop(func: Cursor.() -> Unit) = use {
  while (moveToNext()) {
    func()
  }
}

/** a function that iterates of the rows of a cursor and maps all using a supplied mapper function */
inline fun <T> Cursor.mapRows(mapper: Cursor.() -> T): List<T> = use {
  val list = ArrayList<T>(count)
  while (moveToNext()) {
    list.add(mapper())
  }
  list
}

@SuppressLint("Recycle")
fun SQLiteDatabase.query(
  table: String,
  columns: List<String>? = null,
  selection: String? = null,
  selectionArgs: List<Any>? = null,
  groupBy: String? = null,
  having: String? = null,
  orderBy: String? = null,
  limit: String? = null,
  distinct: Boolean = false
): Cursor {
  val argsAsString = selectionArgs?.map(Any::toString)?.toTypedArray()
  return query(
    distinct,
    table,
    columns?.toTypedArray(),
    selection,
    argsAsString,
    groupBy,
    having,
    orderBy,
    limit
  )
}

fun SQLiteDatabase.update(
  table: String,
  values: ContentValues,
  whereClause: String,
  vararg whereArgs: Any
): Int {
  val whereArgsMapped = whereArgs.map(Any::toString).toTypedArray()
  return update(table, values, whereClause, whereArgsMapped)
}

fun SQLiteDatabase.delete(table: String, whereClause: String, vararg whereArgs: Any): Int {
  val whereArgsMapped = whereArgs.map(Any::toString).toTypedArray()
  return delete(table, whereClause, whereArgsMapped)
}
