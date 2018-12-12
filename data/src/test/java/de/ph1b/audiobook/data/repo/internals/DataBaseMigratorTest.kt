package de.ph1b.audiobook.data.repo.internals

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class DataBaseMigratorTest {

  @Rule
  @JvmField
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    AppDb::class.java.canonicalName,
    FrameworkSQLiteOpenHelperFactory()
  )

  @Test
  fun emptyTableLeadsToCorrectSchema() {
    val dbName = "testDb"
    val db = helper.createDatabase(dbName, 43)
    db.execSQL(BookTable.CREATE_TABLE)
    db.execSQL(ChapterTable.CREATE_TABLE)
    db.execSQL(BookmarkTable.CREATE_TABLE)
    db.close()
    helper.runMigrationsAndValidate(
      dbName,
      AppDb.VERSION,
      true,
      *PersistenceModule().migrations(getApplicationContext())
    )
  }

  @Test
  fun migrate44() {
    val dbName = "testDb"
    val db = helper.createDatabase(dbName, 44)

    data class BookSetting(val id: String, val currentFile: String, val positionInChapter: Int)
    data class Chapter(val file: String, val bookId: String)

    fun insertBookSettings(settings: BookSetting) {
      db.execSQL(
        "INSERT OR REPLACE INTO `bookSettings`(`id`,`currentFile`,`positionInChapter`,`playbackSpeed`,`loudnessGain`,`skipSilence`," +
            "`active`,`lastPlayedAtMillis`) VALUES (?,?,?,?,?,?,?,?)",
        arrayOf(settings.id, settings.currentFile, settings.positionInChapter, 1F, 0, 0, 1, 0)
      )
    }

    fun insertChapter(chapter: Chapter) {
      db.execSQL(
        "INSERT OR REPLACE INTO `chapters`(`file`,`name`,`duration`,`fileLastModified`,`marks`,`bookId`,`id`) " +
            "VALUES (?,?,?,?,?,?,nullif(?, 0))",
        arrayOf(chapter.file, "name", 1L, 0L, "{}", chapter.bookId)
      )
    }

    val correctBookId = "id1"
    val file1 = "file1"
    val file2 = "file2"
    val correctBookSettings = BookSetting(id = correctBookId, currentFile = file1, positionInChapter = 5)
    insertBookSettings(correctBookSettings)
    insertChapter(Chapter(file1, correctBookId))
    insertChapter(Chapter(file2, correctBookId))

    val defectBookId = "id2"
    val defectBookSetting = BookSetting(id = defectBookId, currentFile = file1, positionInChapter = 10)
    insertBookSettings(defectBookSetting)
    insertChapter(Chapter(file2, defectBookId))

    db.close()

    val migratedDb = helper.runMigrationsAndValidate(
      dbName,
      AppDb.VERSION,
      true,
      *PersistenceModule().migrations(getApplicationContext())
    )

    val migratedBookSettings = migratedDb.query("SELECT * FROM bookSettings").mapRows {
      BookSetting(
        id = getString("id"),
        currentFile = getString("currentFile"),
        positionInChapter = getInt("positionInChapter")
      )
    }
    assertThat(migratedBookSettings).containsAllOf(
      correctBookSettings, BookSetting(id = defectBookId, currentFile = file2, positionInChapter = 0)
    )
  }

  @Test
  fun migrate43() {
    val dbName = "testDb"
    val db = helper.createDatabase(dbName, 43)
    db.execSQL(BookTable.CREATE_TABLE)
    db.execSQL(ChapterTable.CREATE_TABLE)
    db.execSQL(BookmarkTable.CREATE_TABLE)

    fun randomString() = UUID.randomUUID().toString()
    fun randomInt() = Random.nextInt(100)

    data class Bookmark(
      val path: String,
      val title: String,
      val time: Int
    )

    val bookmarks = run {
      listOf(
        Bookmark(randomString(), randomString(), randomInt()),
        Bookmark(randomString(), randomString(), randomInt())
      )
    }
    bookmarks.forEach {
      db.insert(BookmarkTable.TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
        put(BookmarkTable.PATH, it.path)
        put(BookmarkTable.TITLE, it.title)
        put(BookmarkTable.TIME, it.time)
      })
    }

    data class Chapter(
      val duration: Int,
      val name: String,
      val path: String,
      val lastModified: Int,
      val marks: String?
    )

    data class Book(
      val author: String?,
      val name: String,
      val currentMediaPath: String,
      val playbackSpeed: Float,
      val root: String,
      val type: String,
      val loudnessGain: Int,
      val active: Int,
      val time: Int,
      val chapters: List<Chapter>
    )

    fun chapters(): List<Chapter> {
      return listOf(
        Chapter(randomInt(), randomString(), randomString(), randomInt(), randomString()),
        Chapter(randomInt(), randomString(), randomString(), randomInt(), null)
      )
    }

    val books = run {
      val firstBook = Book(
        author = randomString(),
        name = randomString(),
        currentMediaPath = randomString(),
        playbackSpeed = 1.1F,
        root = randomString(),
        type = randomString(),
        loudnessGain = randomInt(),
        active = 1,
        time = randomInt(),
        chapters = chapters()
      )
      val secondBook = Book(
        author = null,
        name = randomString(),
        currentMediaPath = randomString(),
        playbackSpeed = 1.1F,
        root = randomString(),
        type = randomString(),
        loudnessGain = randomInt(),
        active = 0,
        time = randomInt(),
        chapters = emptyList()
      )

      listOf(firstBook, secondBook)
    }

    books.forEach { book ->
      val bookId =
        db.insert(BookTable.TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
          put(BookTable.AUTHOR, book.author)
          put(BookTable.NAME, book.name)
          put(BookTable.CURRENT_MEDIA_PATH, book.currentMediaPath)
          put(BookTable.PLAYBACK_SPEED, book.playbackSpeed)
          put(BookTable.ROOT, book.root)
          put(BookTable.TIME, book.time)
          put(BookTable.TYPE, book.type)
          put(BookTable.LOUDNESS_GAIN, book.loudnessGain)
          put(BookTable.ACTIVE, book.active)
        })
      book.chapters.forEach { chapter ->
        db.insert(ChapterTable.TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
          put(ChapterTable.DURATION, chapter.duration)
          put(ChapterTable.NAME, chapter.name)
          put(ChapterTable.PATH, chapter.path)
          put(ChapterTable.LAST_MODIFIED, chapter.lastModified)
          put(ChapterTable.MARKS, chapter.marks)
          put(ChapterTable.BOOK_ID, bookId)
        })
      }
    }
    db.close()

    val migratedDb = helper.runMigrationsAndValidate(
      dbName,
      AppDb.VERSION,
      true,
      *PersistenceModule().migrations(getApplicationContext())
    )

    val metaDataCursor = migratedDb.query("SELECT * FROM bookMetaData")
    val bookSettingsCursor = migratedDb.query("SELECT * FROM bookSettings")

    assertThat(metaDataCursor.count).isEqualTo(books.size)
    assertThat(bookSettingsCursor.count).isEqualTo(books.size)

    books.forEachIndexed { bookIndex, book ->
      metaDataCursor.moveToPosition(bookIndex)
      val metaDataId = metaDataCursor.getString("id")
      assertThat(metaDataCursor.getStringOrNull("author")).isEqualTo(book.author)
      assertThat(metaDataCursor.getString("name")).isEqualTo(book.name)
      assertThat(metaDataCursor.getString("root")).isEqualTo(book.root)

      bookSettingsCursor.moveToPosition(bookIndex)
      val bookSettingsId = bookSettingsCursor.getString("id")
      assertThat(bookSettingsCursor.getString("currentFile")).isEqualTo(book.currentMediaPath)
      assertThat(bookSettingsCursor.getInt("positionInChapter")).isEqualTo(book.time)
      assertThat(bookSettingsCursor.getFloat("playbackSpeed")).isEqualTo(book.playbackSpeed)
      assertThat(bookSettingsCursor.getInt("loudnessGain")).isEqualTo(book.loudnessGain)
      assertThat(bookSettingsCursor.getInt("skipSilence")).isEqualTo(0)
      assertThat(bookSettingsCursor.getInt("active")).isEqualTo(book.active)
      assertThat(bookSettingsCursor.getInt("lastPlayedAtMillis")).isEqualTo(0)

      assertThat(metaDataId).isEqualTo(bookSettingsId)

      val chapterCursor = migratedDb.query("SELECT * FROM chapters WHERE bookId = \"$metaDataId\"")
      assertThat(chapterCursor.count).isEqualTo(book.chapters.size)
      book.chapters.forEachIndexed { chapterIndex, chapter ->
        chapterCursor.moveToPosition(chapterIndex)
        assertThat(chapterCursor.getString("file")).isEqualTo(chapter.path)
        assertThat(chapterCursor.getInt("duration")).isEqualTo(chapter.duration)
        assertThat(chapterCursor.getString("name")).isEqualTo(chapter.name)
        assertThat(chapterCursor.getInt("fileLastModified")).isEqualTo(chapter.lastModified)
        assertThat(chapterCursor.getStringOrNull("marks")).isEqualTo(chapter.marks ?: "{}")
      }
      chapterCursor.close()
    }
    metaDataCursor.close()
    bookSettingsCursor.close()

    val bookmarkCursor = migratedDb.query("SELECT * FROM bookmark")
    assertThat(bookmarkCursor.count).isEqualTo(bookmarks.size)
    bookmarks.forEachIndexed { index, bookmark ->
      bookmarkCursor.moveToPosition(index)
      assertThat(bookmarkCursor.getString("file")).isEqualTo(bookmark.path)
      assertThat(bookmarkCursor.getInt("time")).isEqualTo(bookmark.time)
      assertThat(bookmarkCursor.getString("title")).isEqualTo(bookmark.title)
    }

    bookmarkCursor.close()
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
    const val LOUDNESS_GAIN = "loudnessGain"
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
      $LOUDNESS_GAIN INTEGER,
      $ACTIVE INTEGER NOT NULL DEFAULT 1
    )
  """
  }

  private object ChapterTable {

    const val DURATION = "chapterDuration"
    const val NAME = "chapterName"
    const val PATH = "chapterPath"
    const val TABLE_NAME = "tableChapters"
    const val BOOK_ID = "bookId"
    const val LAST_MODIFIED = "lastModified"
    const val MARKS = "marks"
    const val CREATE_TABLE = """
    CREATE TABLE $TABLE_NAME (
      $DURATION INTEGER NOT NULL,
      $NAME TEXT NOT NULL,
      $PATH TEXT NOT NULL,
      $BOOK_ID INTEGER NOT NULL,
      $LAST_MODIFIED INTEGER NOT NULL,
      $MARKS TEXT,
      FOREIGN KEY ($BOOK_ID) REFERENCES ${BookTable.TABLE_NAME} (${BookTable.ID})
    )
  """
  }

  private object BookmarkTable {

    const val PATH = "bookmarkPath"
    const val TITLE = "bookmarkTitle"
    const val TABLE_NAME = "tableBookmarks"
    const val TIME = "bookmarkTime"
    const val ID = "_id"
    const val CREATE_TABLE = """
    CREATE TABLE $TABLE_NAME (
      $ID INTEGER PRIMARY KEY AUTOINCREMENT,
      $PATH TEXT NOT NULL,
      $TITLE TEXT NOT NULL,
      $TIME INTEGER NOT NULL
    )
  """
  }
}
