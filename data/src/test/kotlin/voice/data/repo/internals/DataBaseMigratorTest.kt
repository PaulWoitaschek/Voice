package voice.data.repo.internals

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import voice.data.allMigrations
import java.util.UUID
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DataBaseMigratorTest {

  @Rule
  @JvmField
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    AppDb::class.java,
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
      *allMigrations(),
    )
  }

  @Test
  fun migrate44() {
    val dbName = "testDb"
    val db = helper.createDatabase(dbName, 44)

    data class BookSetting(
      val id: String,
      val currentFile: String,
      val positionInChapter: Int,
    )
    data class Chapter(
      val file: String,
      val bookId: String,
    )

    fun insertBookSettings(settings: BookSetting) {
      db.execSQL(
        "INSERT OR REPLACE INTO `bookSettings`(`id`,`currentFile`,`positionInChapter`,`playbackSpeed`,`loudnessGain`,`skipSilence`," +
          "`active`,`lastPlayedAtMillis`) VALUES (?,?,?,?,?,?,?,?)",
        arrayOf(settings.id, settings.currentFile, settings.positionInChapter, 1F, 0, 0, 1, 0),
      )
    }

    fun insertChapter(chapter: Chapter) {
      db.execSQL(
        "INSERT OR REPLACE INTO `chapters`(`file`,`name`,`duration`,`fileLastModified`,`marks`,`bookId`,`id`) " +
          "VALUES (?,?,?,?,?,?,nullif(?, 0))",
        arrayOf(chapter.file, "name", 1L, 0L, "{}", chapter.bookId),
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
      45,
      true,
      *allMigrations(),
    )

    val migratedBookSettings = migratedDb.query("SELECT * FROM bookSettings").mapRows {
      BookSetting(
        id = getString("id"),
        currentFile = getString("currentFile"),
        positionInChapter = getInt("positionInChapter"),
      )
    }
    migratedBookSettings.shouldContainExactly(
      correctBookSettings,
      BookSetting(id = defectBookId, currentFile = file2, positionInChapter = 0),
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
      val time: Int,
    )

    val bookmarks = run {
      listOf(
        Bookmark(randomString(), randomString(), randomInt()),
        Bookmark(randomString(), randomString(), randomInt()),
      )
    }
    bookmarks.forEach {
      db.insert(
        BookmarkTable.TABLE_NAME,
        SQLiteDatabase.CONFLICT_FAIL,
        ContentValues().apply {
          put(BookmarkTable.PATH, it.path)
          put(BookmarkTable.TITLE, it.title)
          put(BookmarkTable.TIME, it.time)
        },
      )
    }

    data class Chapter(
      val duration: Int,
      val name: String,
      val path: String,
      val lastModified: Int,
      val marks: String?,
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
      val chapters: List<Chapter>,
    )

    fun chapters(): List<Chapter> {
      return listOf(
        Chapter(randomInt(), randomString(), randomString(), randomInt(), randomString()),
        Chapter(randomInt(), randomString(), randomString(), randomInt(), null),
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
        chapters = chapters(),
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
        chapters = emptyList(),
      )

      listOf(firstBook, secondBook)
    }

    books.forEach { book ->
      val bookId =
        db.insert(
          BookTable.TABLE_NAME,
          SQLiteDatabase.CONFLICT_FAIL,
          ContentValues().apply {
            put(BookTable.AUTHOR, book.author)
            put(BookTable.NAME, book.name)
            put(BookTable.CURRENT_MEDIA_PATH, book.currentMediaPath)
            put(BookTable.PLAYBACK_SPEED, book.playbackSpeed)
            put(BookTable.ROOT, book.root)
            put(BookTable.TIME, book.time)
            put(BookTable.TYPE, book.type)
            put(BookTable.LOUDNESS_GAIN, book.loudnessGain)
            put(BookTable.ACTIVE, book.active)
          },
        )
      book.chapters.forEach { chapter ->
        db.insert(
          ChapterTable.TABLE_NAME,
          SQLiteDatabase.CONFLICT_FAIL,
          ContentValues().apply {
            put(ChapterTable.DURATION, chapter.duration)
            put(ChapterTable.NAME, chapter.name)
            put(ChapterTable.PATH, chapter.path)
            put(ChapterTable.LAST_MODIFIED, chapter.lastModified)
            put(ChapterTable.MARKS, chapter.marks)
            put(ChapterTable.BOOK_ID, bookId)
          },
        )
      }
    }
    db.close()

    val migratedDb = helper.runMigrationsAndValidate(
      dbName,
      44,
      true,
      *allMigrations(),
    )

    val metaDataCursor = migratedDb.query("SELECT * FROM bookMetaData")
    val bookSettingsCursor = migratedDb.query("SELECT * FROM bookSettings")

    metaDataCursor.count shouldBe books.size
    bookSettingsCursor.count shouldBe books.size

    books.forEachIndexed { bookIndex, book ->
      metaDataCursor.moveToPosition(bookIndex)
      val metaDataId = metaDataCursor.getString("id")
      metaDataCursor.getStringOrNull("author") shouldBe book.author
      metaDataCursor.getString("name") shouldBe book.name
      metaDataCursor.getString("root") shouldBe book.root

      bookSettingsCursor.moveToPosition(bookIndex)
      val bookSettingsId = bookSettingsCursor.getString("id")
      bookSettingsCursor.getString("currentFile") shouldBe book.currentMediaPath
      bookSettingsCursor.getInt("positionInChapter") shouldBe book.time
      bookSettingsCursor.getFloat("playbackSpeed") shouldBe book.playbackSpeed
      bookSettingsCursor.getInt("loudnessGain") shouldBe book.loudnessGain
      bookSettingsCursor.getInt("skipSilence") shouldBe 0
      bookSettingsCursor.getInt("active") shouldBe book.active
      bookSettingsCursor.getInt("lastPlayedAtMillis") shouldBe 0

      metaDataId shouldBe bookSettingsId

      val chapterCursor = migratedDb.query("SELECT * FROM chapters WHERE bookId = \"$metaDataId\"")
      chapterCursor.count shouldBe book.chapters.size
      book.chapters.forEachIndexed { chapterIndex, chapter ->
        chapterCursor.moveToPosition(chapterIndex)
        chapterCursor.getString("file") shouldBe chapter.path
        chapterCursor.getInt("duration") shouldBe chapter.duration
        chapterCursor.getString("name") shouldBe chapter.name
        chapterCursor.getInt("fileLastModified") shouldBe chapter.lastModified
        chapterCursor.getStringOrNull("marks") shouldBe (chapter.marks ?: "{}")
      }
      chapterCursor.close()
    }
    metaDataCursor.close()
    bookSettingsCursor.close()

    val bookmarkCursor = migratedDb.query("SELECT * FROM bookmark")
    bookmarkCursor.count shouldBe bookmarks.size
    bookmarks.forEachIndexed { index, bookmark ->
      bookmarkCursor.moveToPosition(index)
      bookmarkCursor.getString("file") shouldBe bookmark.path
      bookmarkCursor.getInt("time") shouldBe bookmark.time
      bookmarkCursor.getString("title") shouldBe bookmark.title
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
