package de.ph1b.audiobook.data.repo.internals.migrations

import android.annotation.SuppressLint
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import de.ph1b.audiobook.data.repo.internals.moveToNextLoop

/**
 * Queries through all books and removes the ones that were added empty by a bug.
 */
@SuppressLint("Recycle")
class Migration30to31 : IncrementalMigration(30) {

  override fun migrate(db: SupportSQLiteDatabase) {
    // book keys
    val bookIdColumn = "bookId"
    val tableBook = "tableBooks"
    val tableChapters = "tableChapters"

    db.query(tableBook, arrayOf(bookIdColumn)).moveToNextLoop {
      val bookId = getLong(0)

      var chapterCount = 0
      val chapterCursor = db.query(
        SupportSQLiteQueryBuilder.builder(tableChapters)
          .selection("$bookIdColumn=?", arrayOf(bookId))
          .create()
      )
      chapterCursor.moveToNextLoop {
        chapterCount++
      }
      if (chapterCount == 0) {
        db.delete(tableBook, "$bookIdColumn=?", arrayOf(bookId.toString()))
      }
    }
  }
}
