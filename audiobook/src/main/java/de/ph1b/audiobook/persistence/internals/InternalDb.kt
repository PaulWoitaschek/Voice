package de.ph1b.audiobook.persistence.internals

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import e
import java.util.*
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
    try {
      val upgradeHelper = DataBaseUpgradeHelper(db)
      upgradeHelper.upgrade(oldVersion)
    } catch (ex: InvalidPropertiesFormatException) {
      e(ex) { "Error at upgrade" }
      BookTable.dropTableIfExists(db)
      ChapterTable.dropTableIfExists(db)
      BookmarkTable.dropTableIfExists(db)
      onCreate(db)
    }
  }

  companion object {

    private val DATABASE_VERSION = 36
    private val DATABASE_NAME = "autoBookDB"
  }
}
