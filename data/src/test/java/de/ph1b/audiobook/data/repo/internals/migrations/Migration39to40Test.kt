package de.ph1b.audiobook.data.repo.internals.migrations

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.data.repo.internals.getInt
import de.ph1b.audiobook.data.repo.internals.mapRows
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class Migration39to40Test {

  private lateinit var db: SupportSQLiteDatabase
  private lateinit var helper: SupportSQLiteOpenHelper

  @Before
  fun setUp() {
    val config = SupportSQLiteOpenHelper.Configuration
      .builder(getApplicationContext())
      .callback(object : SupportSQLiteOpenHelper.Callback(39) {
        override fun onCreate(db: SupportSQLiteDatabase) {
          db.execSQL(BookTable.CREATE_TABLE)
        }

        override fun onUpgrade(db: SupportSQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        }
      })
      .build()
    helper = FrameworkSQLiteOpenHelperFactory().create(config)
    db = helper.writableDatabase
  }

  @After
  fun tearDown() {
    helper.close()
  }

  @Test
  fun negativeNumbersBecomeZero() {
    val bookCvWithNegativeTime = contentValuesForBookWithTime(-100)
    db.insert(BookTable.TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL, bookCvWithNegativeTime)

    val bookCvWithPositiveTime = contentValuesForBookWithTime(5000)
    db.insert(BookTable.TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL, bookCvWithPositiveTime)

    Migration39to40().migrate(db)

    val query = SupportSQLiteQueryBuilder.builder(BookTable.TABLE_NAME)
      .columns(arrayOf(BookTable.TIME))
      .create()
    val times = db.query(query)
      .mapRows { getInt(BookTable.TIME) }
    assertThat(times).containsExactly(0, 5000)
  }

  @SuppressLint("SdCardPath")
  private fun contentValuesForBookWithTime(time: Int) = ContentValues().apply {
    put(BookTable.NAME, "firstBookName")
    put(BookTable.CURRENT_MEDIA_PATH, "/sdcard/file1.mp3")
    put(BookTable.PLAYBACK_SPEED, 1F)
    put(BookTable.ROOT, "/sdcard")
    put(BookTable.TIME, time)
    put(BookTable.TYPE, "COLLECTION_FOLDER")
  }

  object BookTable {
    const val ID = "bookId"
    const val NAME = "bookName"
    const val AUTHOR = "bookAuthor"
    const val CURRENT_MEDIA_PATH = "bookCurrentMediaPath"
    const val PLAYBACK_SPEED = "bookSpeed"
    const val ROOT = "bookRoot"
    const val TIME = "bookTime"
    const val TYPE = "bookType"
    const val ACTIVE = "BOOK_ACTIVE"
    const val TABLE_NAME = "tableBooks"
    const val CREATE_TABLE = """
      CREATE TABLE $TABLE_NAME (
        $ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $NAME TEXT NOT NULL,
        $AUTHOR TEXT,
        $CURRENT_MEDIA_PATH TEXT NOT NULL,
        $PLAYBACK_SPEED REAL NOT NULL,
        $ROOT TEXT NOT NULL,
        $TIME INTEGER NOT NULL,
        $TYPE TEXT NOT NULL,
        $ACTIVE INTEGER NOT NULL DEFAULT 1
      )
    """
  }
}
