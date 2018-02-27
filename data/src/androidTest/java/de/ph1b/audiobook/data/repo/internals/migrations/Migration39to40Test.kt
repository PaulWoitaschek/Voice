package de.ph1b.audiobook.data.repo.internals.migrations

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.support.test.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.data.repo.internals.mapRows
import de.ph1b.audiobook.data.repo.internals.query
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Test for the database 39->40 version migration
 */
class Migration39to40Test {

  private lateinit var db: SQLiteDatabase
  private lateinit var dbHelper: SQLiteOpenHelper

  @Before
  fun setUp() {
    val context = InstrumentationRegistry.getTargetContext()
    dbHelper = DBHelper(context)
    context.deleteDatabase(dbHelper.databaseName)
    db = dbHelper.writableDatabase
  }

  @After
  fun tearDown() {
    val context = InstrumentationRegistry.getTargetContext()
    context.deleteDatabase(dbHelper.databaseName)
  }

  @Test
  fun negativeNumbersBecomeZero() {
    val bookCvWithNegativeTime = contentValuesForBookWithTime(-100)
    db.insertOrThrow(BookTable.TABLE_NAME, null, bookCvWithNegativeTime)

    val bookCvWithPositiveTime = contentValuesForBookWithTime(5000)
    db.insertOrThrow(BookTable.TABLE_NAME, null, bookCvWithPositiveTime)

    Migration39to40().migrate(db)

    val times = db.query(BookTable.TABLE_NAME, listOf(BookTable.TIME))
      .mapRows { getInt(getColumnIndexOrThrow(BookTable.TIME)) }
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

  class DBHelper(context: Context) : SQLiteOpenHelper(context, "testDb", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
      BookTable.onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
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
    private const val CREATE_TABLE = """
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

    fun onCreate(db: SQLiteDatabase) {
      db.execSQL(CREATE_TABLE)
    }
  }
}
