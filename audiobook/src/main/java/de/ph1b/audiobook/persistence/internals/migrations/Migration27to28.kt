package de.ph1b.audiobook.persistence.internals.migrations

import android.database.sqlite.SQLiteDatabase

/**
 * Deletes the table if that failed previously due to a bug in [.upgrade26]
 *
 * @author Paul Woitaschek
 */
class Migration27to28 : Migration {

  override fun migrate(db: SQLiteDatabase) {
    db.execSQL("DROP TABLE IF EXISTS TABLE_BOOK_COPY")
  }
}