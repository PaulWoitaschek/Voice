package de.ph1b.audiobook.data.repo.internals.migrations

import android.arch.persistence.db.SupportSQLiteDatabase

/**
 * Deletes the table if that failed previously due to a bug in [.upgrade26]
 */
class Migration27to28 : IncrementalMigration(27) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("DROP TABLE IF EXISTS TABLE_BOOK_COPY")
  }
}
