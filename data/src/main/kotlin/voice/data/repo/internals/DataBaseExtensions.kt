package voice.data.repo.internals

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.withContext
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun Cursor.moveToNextLoop(func: Cursor.() -> Unit) = use {
  moveToPosition(-1)
  while (moveToNext()) {
    func()
  }
}

inline fun Cursor.consumeEach(consume: (Cursor) -> Unit) = use {
  moveToPosition(-1)
  while (moveToNext()) {
    consume(it)
  }
}

/** a function that iterates of the rows of a cursor and maps all using a supplied mapper function */
inline fun <T> Cursor.mapRows(mapper: Cursor.() -> T): List<T> = use {
  moveToPosition(-1)
  val list = ArrayList<T>(count)
  while (moveToNext()) {
    list.add(mapper())
  }
  list
}

inline fun <T> SupportSQLiteDatabase.transaction(
  exclusive: Boolean = true,
  body: SupportSQLiteDatabase.() -> T,
): T {
  if (exclusive) {
    beginTransaction()
  } else {
    beginTransactionNonExclusive()
  }
  try {
    val result = body()
    setTransactionSuccessful()
    return result
  } finally {
    endTransaction()
  }
}

suspend inline fun <T> RoomDatabase.transaction(crossinline action: suspend () -> T): T {
  contract {
    callsInPlace(action, InvocationKind.EXACTLY_ONCE)
  }
  return withContext(kotlinx.coroutines.Dispatchers.IO) {
    @Suppress("DEPRECATION")
    beginTransaction()
    try {
      action().also {
        @Suppress("DEPRECATION")
        setTransactionSuccessful()
      }
    } finally {
      @Suppress("DEPRECATION")
      endTransaction()
    }
  }
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
  distinct: Boolean = false,
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
    limit,
  )
}

fun SQLiteDatabase.update(
  table: String,
  values: ContentValues,
  whereClause: String,
  vararg whereArgs: Any,
): Int {
  val whereArgsMapped = whereArgs.map(Any::toString).toTypedArray()
  return update(table, values, whereClause, whereArgsMapped)
}

fun SQLiteDatabase.delete(table: String, whereClause: String, vararg whereArgs: Any): Int {
  val whereArgsMapped = whereArgs.map(Any::toString).toTypedArray()
  return delete(table, whereClause, whereArgsMapped)
}
