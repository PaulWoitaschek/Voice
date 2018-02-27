package de.ph1b.audiobook.data.repo.internals.migrations

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import de.ph1b.audiobook.data.repo.internals.moveToNextLoop
import org.json.JSONObject
import java.util.*

@SuppressLint("Recycle")
class Migration29to30 : Migration {

  // tables
  private val TABLE_BOOK = "tableBooks"
  private val TABLE_CHAPTERS = "tableChapters"
  private val TABLE_BOOKMARKS = "tableBookmarks"

  private val BOOK_ID = "bookId"
  private val BOOK_NAME = "bookName"
  private val BOOK_AUTHOR = "bookAuthor"
  private val BOOK_CURRENT_MEDIA_PATH = "bookCurrentMediaPath"
  private val BOOK_PLAYBACK_SPEED = "bookSpeed"
  private val BOOK_ROOT = "bookRoot"
  private val BOOK_TIME = "bookTime"
  private val BOOK_TYPE = "bookType"
  private val BOOK_USE_COVER_REPLACEMENT = "bookUseCoverReplacement"
  private val BOOK_ACTIVE = "BOOK_ACTIVE"

  // chapter keys
  private val CHAPTER_DURATION = "chapterDuration"
  private val CHAPTER_NAME = "chapterName"
  private val CHAPTER_PATH = "chapterPath"

  // bookmark keys
  private val BOOKMARK_TIME = "bookmarkTime"
  private val BOOKMARK_PATH = "bookmarkPath"
  private val BOOKMARK_TITLE = "bookmarkTitle"

  private val CREATE_TABLE_BOOK = """
    CREATE TABLE $TABLE_BOOK (
      $BOOK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
      $BOOK_NAME TEXT NOT NULL, $BOOK_AUTHOR TEXT,
      $BOOK_CURRENT_MEDIA_PATH TEXT NOT NULL,
      $BOOK_PLAYBACK_SPEED REAL NOT NULL,
      $BOOK_ROOT TEXT NOT NULL,
      $BOOK_TIME INTEGER NOT NULL,
      $BOOK_TYPE TEXT NOT NULL,
      $BOOK_USE_COVER_REPLACEMENT INTEGER NOT NULL,
      $BOOK_ACTIVE INTEGER NOT NULL DEFAULT 1
    )
  """

  private val CREATE_TABLE_CHAPTERS = """
    CREATE TABLE $TABLE_CHAPTERS (
      $CHAPTER_DURATION INTEGER NOT NULL,
      $CHAPTER_NAME TEXT NOT NULL,
      $CHAPTER_PATH TEXT NOT NULL,
      $BOOK_ID INTEGER NOT NULL,
      FOREIGN KEY ($BOOK_ID) REFERENCES $TABLE_BOOK($BOOK_ID)
    )
  """

  private val CREATE_TABLE_BOOKMARKS = """
    CREATE TABLE $TABLE_BOOKMARKS (
      $BOOKMARK_PATH TEXT NOT NULL,
      $BOOKMARK_TITLE TEXT NOT NULL,
      $BOOKMARK_TIME INTEGER NOT NULL,
      $BOOK_ID INTEGER NOT NULL,
      FOREIGN KEY ($BOOK_ID) REFERENCES $TABLE_BOOK($BOOK_ID)
    )
  """

  override fun migrate(db: SQLiteDatabase) {
    // fetching old contents
    val cursor = db.query(
      "TABLE_BOOK", arrayOf("BOOK_JSON", "BOOK_ACTIVE"),
      null, null, null, null, null
    )
    val bookContents = ArrayList<String>(cursor.count)
    val activeMapping = ArrayList<Boolean>(cursor.count)
    cursor.moveToNextLoop {
      bookContents.add(cursor.getString(0))
      activeMapping.add(cursor.getInt(1) == 1)
    }
    db.execSQL("DROP TABLE TABLE_BOOK")

    // drop tables in case they exist
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOK)
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAPTERS)
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS)

    // create new tables
    db.execSQL(CREATE_TABLE_BOOK)
    db.execSQL(CREATE_TABLE_CHAPTERS)
    db.execSQL(CREATE_TABLE_BOOKMARKS)

    for (i in bookContents.indices) {
      val bookJson = bookContents[i]
      val bookActive = activeMapping[i]

      val bookObj = JSONObject(bookJson)
      val bookmarks = bookObj.getJSONArray("bookmarks")
      val chapters = bookObj.getJSONArray("chapters")
      val currentMediaPath = bookObj.getString("currentMediaPath")
      val bookName = bookObj.getString("name")
      val speed = bookObj.getDouble("playbackSpeed").toFloat()
      val root = bookObj.getString("root")
      val time = bookObj.getInt("time")
      val type = bookObj.getString("type")
      val useCoverReplacement = bookObj.getBoolean("useCoverReplacement")

      val bookCV = ContentValues()
      bookCV.put(BOOK_CURRENT_MEDIA_PATH, currentMediaPath)
      bookCV.put(BOOK_NAME, bookName)
      bookCV.put(BOOK_PLAYBACK_SPEED, speed)
      bookCV.put(BOOK_ROOT, root)
      bookCV.put(BOOK_TIME, time)
      bookCV.put(BOOK_TYPE, type)
      bookCV.put(BOOK_USE_COVER_REPLACEMENT, if (useCoverReplacement) 1 else 0)
      bookCV.put(BOOK_ACTIVE, if (bookActive) 1 else 0)

      val bookId = db.insert(TABLE_BOOK, null, bookCV)

      for (j in 0 until chapters.length()) {
        val chapter = chapters.getJSONObject(j)
        val chapterDuration = chapter.getInt("duration")
        val chapterName = chapter.getString("name")
        val chapterPath = chapter.getString("path")

        val chapterCV = ContentValues()
        chapterCV.put(CHAPTER_DURATION, chapterDuration)
        chapterCV.put(CHAPTER_NAME, chapterName)
        chapterCV.put(CHAPTER_PATH, chapterPath)
        chapterCV.put(BOOK_ID, bookId)

        db.insert(TABLE_CHAPTERS, null, chapterCV)
      }

      for (j in 0 until bookmarks.length()) {
        val bookmark = bookmarks.getJSONObject(j)
        val bookmarkTime = bookmark.getInt("time")
        val bookmarkPath = bookmark.getString("mediaPath")
        val bookmarkTitle = bookmark.getString("title")

        val bookmarkCV = ContentValues()
        bookmarkCV.put(BOOKMARK_PATH, bookmarkPath)
        bookmarkCV.put(BOOKMARK_TITLE, bookmarkTitle)
        bookmarkCV.put(BOOKMARK_TIME, bookmarkTime)
        bookmarkCV.put(BOOK_ID, bookId)

        db.insert(TABLE_BOOKMARKS, null, bookmarkCV)
      }
    }
  }
}
