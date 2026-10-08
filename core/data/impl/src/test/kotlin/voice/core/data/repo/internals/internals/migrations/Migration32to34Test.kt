package voice.core.data.repo.internals.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import voice.core.data.repo.internals.getLong
import voice.core.data.repo.internals.getString
import voice.core.data.repo.internals.mapRows
import voice.core.data.repo.internals.migrations.Migration32to34
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class Migration32to34Test {

  private lateinit var db: SupportSQLiteDatabase
  private lateinit var helper: SupportSQLiteOpenHelper

  @Before
  fun setUp() {
    val config = SupportSQLiteOpenHelper.Configuration
      .builder(getApplicationContext())
      .callback(
        object : SupportSQLiteOpenHelper.Callback(32) {
          override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(BookmarkTable.CREATE_TABLE)
          }

          override fun onUpgrade(
            db: SupportSQLiteDatabase,
            oldVersion: Int,
            newVersion: Int,
          ) {
          }
        },
      )
      .build()
    helper = FrameworkSQLiteOpenHelperFactory().create(config)
    db = helper.writableDatabase
  }

  @After
  fun tearDown() {
    helper.close()
  }

  @Test
  fun `bookmarks are kept`() {
    db.insert(BookmarkTable.TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL, bookmarkContentValues("Intro", 100))
    db.insert(BookmarkTable.TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL, bookmarkContentValues("Outro", 5000))

    Migration32to34().migrate(db)

    val bookmarks = db.query("SELECT * FROM ${BookmarkTable.TABLE_NAME}")
      .mapRows {
        Triple(getString(BookmarkTable.PATH), getString(BookmarkTable.TITLE), getLong(BookmarkTable.TIME))
      }
    assertEquals(
      expected = listOf(
        Triple("/sdcard/file1.mp3", "Intro", 100L),
        Triple("/sdcard/file1.mp3", "Outro", 5000L),
      ),
      actual = bookmarks,
    )
  }

  private fun bookmarkContentValues(
    title: String,
    time: Long,
  ) = ContentValues().apply {
    put(BookmarkTable.PATH, "/sdcard/file1.mp3")
    put(BookmarkTable.TITLE, title)
    put(BookmarkTable.TIME, time)
    put(BookmarkTable.BOOK_ID, 1)
  }

  private object BookmarkTable {
    const val PATH = "bookmarkPath"
    const val TITLE = "bookmarkTitle"
    const val TIME = "bookmarkTime"
    const val BOOK_ID = "bookId"
    const val TABLE_NAME = "tableBookmarks"
    const val CREATE_TABLE = """
      CREATE TABLE $TABLE_NAME (
        $PATH TEXT NOT NULL,
        $TITLE TEXT NOT NULL,
        $TIME INTEGER NOT NULL,
        $BOOK_ID INTEGER NOT NULL
      )
    """
  }
}
