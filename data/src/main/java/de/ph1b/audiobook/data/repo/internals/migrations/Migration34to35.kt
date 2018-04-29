package de.ph1b.audiobook.data.repo.internals.migrations

import android.arch.persistence.db.SupportSQLiteDatabase

/**
 * Due to a bug negative book ids were inserted
 */
class Migration34to35 : IncrementalMigration(34) {

  private val TABLE_NAME = "tableBooks"

  override fun migrate(db: SupportSQLiteDatabase) {
    db.delete(TABLE_NAME, "bookId<=", arrayOf(-1))
  }
}
