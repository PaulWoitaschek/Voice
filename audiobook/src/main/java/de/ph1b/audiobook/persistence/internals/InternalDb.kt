package de.ph1b.audiobook.persistence.internals

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class that manages the underlying the database.
 *
 * @author Paul Woitaschek
 */
@Singleton class InternalDb
@Inject constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

  override fun onCreate(db: SQLiteDatabase) {
    BookTable.onCreate(db)
    ChapterTable.onCreate(db)
    BookmarkTable.onCreate(db)
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    val migrator = DataBaseMigrator(db)
    migrator.upgrade(oldVersion, newVersion)
  }

  companion object {

    private const val DATABASE_VERSION = 40
    private const val DATABASE_NAME = "autoBookDB"
  }
}
