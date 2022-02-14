package de.ph1b.audiobook.data.repo.internals.migrations

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import de.ph1b.audiobook.data.repo.internals.moveToNextLoop

/**
 * Corrects media paths that have been falsely set.
 */
@SuppressLint("Recycle")
class Migration31to32 : IncrementalMigration(31) {

  private val BOOK_ID = "bookId"
  private val TABLE_BOOK = "tableBooks"
  private val TABLE_CHAPTERS = "tableChapters"
  private val BOOK_CURRENT_MEDIA_PATH = "bookCurrentMediaPath"
  private val CHAPTER_PATH = "chapterPath"

  override fun migrate(db: SupportSQLiteDatabase) {
    db.query(
      TABLE_BOOK,
      arrayOf(BOOK_ID, BOOK_CURRENT_MEDIA_PATH)
    ).moveToNextLoop {
      val bookId = getLong(0)
      val bookmarkCurrentMediaPath = getString(1)

      val chapterCursor = db.query(
        SupportSQLiteQueryBuilder.builder(TABLE_CHAPTERS)
          .columns(arrayOf(CHAPTER_PATH))
          .selection("$BOOK_ID=?", arrayOf(bookId))
          .create()
      )
      val chapterPaths = ArrayList<String>(chapterCursor.count)
      chapterCursor.moveToNextLoop {
        val chapterPath = chapterCursor.getString(0)
        chapterPaths.add(chapterPath)
      }

      if (chapterPaths.isEmpty()) {
        db.delete(TABLE_BOOK, "$BOOK_ID=?", arrayOf(bookId.toString()))
      } else {
        val mediaPathValid = chapterPaths.contains(bookmarkCurrentMediaPath)
        if (!mediaPathValid) {
          val cv = ContentValues()
          cv.put(BOOK_CURRENT_MEDIA_PATH, chapterPaths.first())
          db.update(
            TABLE_BOOK,
            SQLiteDatabase.CONFLICT_FAIL,
            cv,
            "$BOOK_ID=?",
            arrayOf(bookId.toString())
          )
        }
      }
    }
  }
}
