package de.ph1b.audiobook.persistence.internals.migrations

import android.database.sqlite.SQLiteDatabase

/**
 * Due to a bug negative book ids were inserted
 */
class Migration34to35 : Migration {

  private val TABLE_NAME = "tableBooks"

  override fun migrate(db: SQLiteDatabase) {
    db.delete(TABLE_NAME, "bookId<=-1", null)
  }
}
