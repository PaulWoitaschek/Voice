package de.ph1b.audiobook.data.repo.internals.migrations

import android.database.sqlite.SQLiteDatabase

/**
 * Drops all tables and creates new ones.
 */
class Migration23to24 : Migration {

  override fun migrate(db: SQLiteDatabase) {
    db.execSQL("DROP TABLE IF EXISTS TABLE_BOOK")
    db.execSQL("DROP TABLE IF EXISTS TABLE_CHAPTERS")

    db.execSQL(
      """
      CREATE TABLE TABLE_BOOK (
        BOOK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        BOOK_TYPE TEXT NOT NULL,
        BOOK_ROOT TEXT NOT NULL
      )
    """
    )
    db.execSQL(
      """
      CREATE TABLE TABLE_CHAPTERS (
        CHAPTER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        CHAPTER_PATH TEXT NOT NULL,
        CHAPTER_DURATION INTEGER NOT NULL,
        CHAPTER_NAME TEXT NOT NULL,
        BOOK_ID INTEGER NOT NULL,
        FOREIGN KEY(BOOK_ID) REFERENCES TABLE_BOOK(BOOK_ID)
    )
    """
    )
  }
}
