package de.ph1b.audiobook.data.repo.internals.migrations

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import de.ph1b.audiobook.data.repo.internals.moveToNextLoop
import org.json.JSONObject
import java.util.ArrayList

// tables
private const val TABLE_BOOK = "tableBooks"
private const val TABLE_CHAPTERS = "tableChapters"
private const val TABLE_BOOKMARKS = "tableBookmarks"

private const val BOOK_ID = "bookId"
private const val BOOK_NAME = "bookName"
private const val BOOK_AUTHOR = "bookAuthor"
private const val BOOK_CURRENT_MEDIA_PATH = "bookCurrentMediaPath"
private const val BOOK_PLAYBACK_SPEED = "bookSpeed"
private const val BOOK_ROOT = "bookRoot"
private const val BOOK_TIME = "bookTime"
private const val BOOK_TYPE = "bookType"
private const val BOOK_USE_COVER_REPLACEMENT = "bookUseCoverReplacement"
private const val BOOK_ACTIVE = "BOOK_ACTIVE"

// chapter keys
private const val CHAPTER_DURATION = "chapterDuration"
private const val CHAPTER_NAME = "chapterName"
private const val CHAPTER_PATH = "chapterPath"

// bookmark keys
private const val BOOKMARK_TIME = "bookmarkTime"
private const val BOOKMARK_PATH = "bookmarkPath"
private const val BOOKMARK_TITLE = "bookmarkTitle"

private const val CREATE_TABLE_BOOK = """
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

private const val CREATE_TABLE_CHAPTERS = """
    CREATE TABLE $TABLE_CHAPTERS (
      $CHAPTER_DURATION INTEGER NOT NULL,
      $CHAPTER_NAME TEXT NOT NULL,
      $CHAPTER_PATH TEXT NOT NULL,
      $BOOK_ID INTEGER NOT NULL,
      FOREIGN KEY ($BOOK_ID) REFERENCES $TABLE_BOOK($BOOK_ID)
    )
  """

private const val CREATE_TABLE_BOOKMARKS = """
    CREATE TABLE $TABLE_BOOKMARKS (
      $BOOKMARK_PATH TEXT NOT NULL,
      $BOOKMARK_TITLE TEXT NOT NULL,
      $BOOKMARK_TIME INTEGER NOT NULL,
      $BOOK_ID INTEGER NOT NULL,
      FOREIGN KEY ($BOOK_ID) REFERENCES $TABLE_BOOK($BOOK_ID)
    )
  """

@SuppressLint("Recycle")
class Migration29to30 : IncrementalMigration(29) {


  override fun migrate(db: SupportSQLiteDatabase) {
    // fetching old contents
    val cursor = db.query(
      "TABLE_BOOK", arrayOf("BOOK_JSON", "BOOK_ACTIVE")
    )
    val bookContents = ArrayList<String>(cursor.count)
    val activeMapping = ArrayList<Boolean>(cursor.count)
    cursor.moveToNextLoop {
      bookContents.add(cursor.getString(0))
      activeMapping.add(cursor.getInt(1) == 1)
    }
    db.execSQL("DROP TABLE TABLE_BOOK")

    // drop tables in case they exist
    db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOK")
    db.execSQL("DROP TABLE IF EXISTS $TABLE_CHAPTERS")
    db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKMARKS")

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

      val bookId = db.insert(TABLE_BOOK, SQLiteDatabase.CONFLICT_FAIL, bookCV)

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

        db.insert(TABLE_CHAPTERS, SQLiteDatabase.CONFLICT_FAIL, chapterCV)
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

        db.insert(TABLE_BOOKMARKS, SQLiteDatabase.CONFLICT_FAIL, bookmarkCV)
      }
    }
  }
}
