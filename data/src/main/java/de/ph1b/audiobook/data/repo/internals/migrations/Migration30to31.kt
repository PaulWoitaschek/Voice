package de.ph1b.audiobook.data.repo.internals.migrations

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase
import de.ph1b.audiobook.data.repo.internals.moveToNextLoop

/**
 * Queries through all books and removes the ones that were added empty by a bug.
 */
@SuppressLint("Recycle")
class Migration30to31 : Migration {

  override fun migrate(db: SQLiteDatabase) {
    // book keys
    val bookIdColumn = "bookId"
    val tableBook = "tableBooks"
    val tableChapters = "tableChapters"

    db.query(tableBook, arrayOf(bookIdColumn), null, null, null, null, null).moveToNextLoop {
      val bookId = getLong(0)

      var chapterCount = 0
      val chapterCursor = db.query(
        tableChapters,
        null,
        bookIdColumn + "=?",
        arrayOf(bookId.toString()),
        null, null, null
      )
      chapterCursor.moveToNextLoop {
        chapterCount++
      }
      if (chapterCount == 0) {
        db.delete(tableBook, bookIdColumn + "=?", arrayOf(bookId.toString()))
      }
    }
  }
}
