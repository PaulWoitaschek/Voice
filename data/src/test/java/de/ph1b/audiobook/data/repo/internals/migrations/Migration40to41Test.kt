package de.ph1b.audiobook.data.repo.internals.migrations

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
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
import org.robolectric.annotation.Config

/**
 * Test the migration from 40 to 41
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class Migration40to41Test {

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

  @Test(expected = SQLiteException::class)
  fun insertBeforeMigrationThrows() {
    val bookCv = bookContentValues()
    bookCv.put("loudnessGain", 100)
    db.insert(BookTable.TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL, bookCv)
  }

  @Test
  fun insert() {
    val bookCv = bookContentValues()
    val id = db.insert(BookTable.TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL, bookCv)

    Migration40to41().migrate(db)

    val loudnessGainCv = ContentValues().apply {
      put("loudnessGain", 100)
    }
    db.update(
      BookTable.TABLE_NAME,
      SQLiteDatabase.CONFLICT_FAIL,
      loudnessGainCv,
      "${BookTable.ID}=?",
      arrayOf(id)
    )

    val query = SupportSQLiteQueryBuilder.builder(BookTable.TABLE_NAME)
      .columns(arrayOf("loudnessGain"))
      .create()
    val loudnessGains = db.query(query)
      .mapRows {
        getInt("loudnessGain")
      }
    assertThat(loudnessGains).containsExactly(100)
  }

  @SuppressLint("SdCardPath")
  private fun bookContentValues() = ContentValues().apply {
    put(BookTable.NAME, "firstBookName")
    put(BookTable.CURRENT_MEDIA_PATH, "/sdcard/file1.mp3")
    put(BookTable.PLAYBACK_SPEED, 1F)
    put(BookTable.ROOT, "/sdcard")
    put(BookTable.TIME, 500)
    put(BookTable.TYPE, "COLLECTION_FOLDER")
  }

  @After
  fun tearDown() {
    helper.close()
  }

  private object BookTable {
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
