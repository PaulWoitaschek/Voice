package de.ph1b.audiobook.persistence.internals

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.support.test.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Tests for the data base migration
 */
class DataBaseMigratorTest {

  @Test
  fun testUpgradeFromZeroLeadsToEmptyDatabase() {
    val context = InstrumentationRegistry.getTargetContext()
    val helper = TestSQLiteHelper(context)
    context.deleteDatabase(helper.databaseName)
    val upgrade = DataBaseMigrator(helper.writableDatabase)

    val currentVersion = InternalDb(context).readableDatabase.version

    upgrade.upgrade(1, currentVersion)

    val bookTableQuery = helper.writableDatabase.query(BookTable.TABLE_NAME)
    assertThat(bookTableQuery.count).isEqualTo(0)

    val bookmarkTableQuery = helper.writableDatabase.query(BookmarkTable.TABLE_NAME)
    assertThat(bookmarkTableQuery.count).isEqualTo(0)

    val chapterTableQuery = helper.writableDatabase.query(ChapterTable.TABLE_NAME)
    assertThat(chapterTableQuery.count).isEqualTo(0)
  }

  class TestSQLiteHelper(context: Context) : SQLiteOpenHelper(context, "testDb", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
      db.execSQL(
          """
      CREATE TABLE TABLE_BOOK (
        RANDOM INTEGER PRIMARY KEY AUTOINCREMENT,
        TYPE TEXT NOT NULL,
        NOBODY TEXT NOT NULL
      )
    """
      )
      db.insertOrThrow(
          "TABLE_BOOK", null, ContentValues().apply {
        put("TYPE", "trashType")
        put("NOBODY", "someText")
      }
      )
      db.execSQL(
          """
      CREATE TABLE TABLE_CHAPTERS (
        PETER INTEGER PRIMARY KEY AUTOINCREMENT,
        BOB TEXT NOT NULL,
        AUGUST INTEGER NOT NULL
    )
    """
      )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }
  }
}
