package de.ph1b.audiobook.data.repo.internals

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.support.test.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.data.repo.internals.tables.BookTable
import de.ph1b.audiobook.data.repo.internals.tables.ChapterTable
import org.junit.Test

class DataBaseMigratorTest {

  @Test
  fun upgradeFromZeroLeadsToEmptyDatabase() {
    val context = InstrumentationRegistry.getTargetContext()
    val helper = TestSQLiteHelper(context)
    val upgrade = DataBaseMigrator(helper.writableDatabase, context)

    val currentVersion = InternalDb(context).readableDatabase.version

    upgrade.upgrade(1, currentVersion)

    val bookTableQuery = helper.writableDatabase.query(BookTable.TABLE_NAME)
    assertThat(bookTableQuery.count).isEqualTo(0)

    val bookmarkTableQuery = helper.writableDatabase.query("tableBookmarks")
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
