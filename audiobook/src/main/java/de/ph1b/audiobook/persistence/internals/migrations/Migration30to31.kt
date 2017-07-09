package de.ph1b.audiobook.persistence.internals.migrations

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase
import de.ph1b.audiobook.persistence.internals.moveToNextLoop

/**
 * Queries through all books and removes the ones that were added empty by a bug.
 */
@SuppressLint("Recycle")
class Migration30to31 : Migration {

  override fun migrate(db: SQLiteDatabase) {
    // book keys
    val BOOK_ID = "bookId"
    val TABLE_BOOK = "tableBooks"
    val TABLE_CHAPTERS = "tableChapters"

    db.query(TABLE_BOOK, arrayOf(BOOK_ID), null, null, null, null, null).moveToNextLoop {
      val bookId = getLong(0)

      var chapterCount = 0
      val chapterCursor = db.query(TABLE_CHAPTERS,
          null,
          BOOK_ID + "=?",
          arrayOf(bookId.toString()),
          null, null, null)
      chapterCursor.moveToNextLoop {
        chapterCount++
      }
      if (chapterCount == 0) {
        db.delete(TABLE_BOOK, BOOK_ID + "=?", arrayOf(bookId.toString()))
      }
    }
  }
}
