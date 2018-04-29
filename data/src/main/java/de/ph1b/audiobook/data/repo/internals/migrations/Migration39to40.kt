package de.ph1b.audiobook.data.repo.internals.migrations

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.OnConflictStrategy
import android.content.ContentValues

/**
 * From DB version 39, the position of a book must no longer be negative. So all negative positions
 * get set to 0.
 */
class Migration39to40 : IncrementalMigration(39) {

  private val BOOK_TABLE_NAME = "tableBooks"
  private val BOOK_TIME = "bookTime"

  override fun migrate(db: SupportSQLiteDatabase) {
    val positionZeroContentValues = ContentValues().apply {
      put(BOOK_TIME, 0)
    }
    db.update(
      BOOK_TABLE_NAME,
      OnConflictStrategy.FAIL,
      positionZeroContentValues,
      "$BOOK_TIME < ?",
      arrayOf(0)
    )
  }
}
