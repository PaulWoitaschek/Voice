package voice.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.sqlite.db.SupportSQLiteDatabase
import voice.data.repo.internals.consumeEach
import voice.data.repo.internals.getFloat
import voice.data.repo.internals.getInt
import voice.data.repo.internals.getIntOrNull
import voice.data.repo.internals.getLong
import voice.data.repo.internals.getString
import voice.data.repo.internals.getStringOrNull
import voice.data.repo.internals.moveToNextLoop
import java.util.UUID

/**
 * Initial Room Migration
 */
class Migration43to44 : IncrementalMigration(43) {

  override fun migrate(db: SupportSQLiteDatabase) {
    createNewTables(db)
    fill(db)
    deleteOldTables(db)
  }

  private fun createNewTables(db: SupportSQLiteDatabase) {
    db.execSQL(
      """
      CREATE TABLE `bookmark` (
      `file` TEXT NOT NULL,
      `title` TEXT NOT NULL,
      `time` INTEGER NOT NULL,
      `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)
      """.trimIndent()
    )
    db.execSQL(
      """
      CREATE TABLE `chapters` (
      `file` TEXT NOT NULL,
      `name` TEXT NOT NULL,
      `duration` INTEGER NOT NULL,
      `fileLastModified` INTEGER NOT NULL,
      `marks` TEXT NOT NULL,
      `bookId` TEXT NOT NULL,
      `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)
      """.trimIndent()
    )
    db.execSQL("CREATE  INDEX `index_chapters_bookId` ON `chapters` (`bookId`)")
    db.execSQL(
      """
      CREATE TABLE `bookMetaData` (
      `id` TEXT NOT NULL,
      `type` TEXT NOT NULL,
      `author` TEXT,
      `name` TEXT NOT NULL,
      `root` TEXT NOT NULL,
      `addedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))
      """.trimIndent()
    )
    db.execSQL(
      """
      CREATE TABLE `bookSettings` (
      `id` TEXT NOT NULL,
      `currentFile` TEXT NOT NULL,
      `positionInChapter` INTEGER NOT NULL,
      `playbackSpeed` REAL NOT NULL,
      `loudnessGain` INTEGER NOT NULL,
      `skipSilence` INTEGER NOT NULL,
      `active` INTEGER NOT NULL,
      `lastPlayedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))
      """.trimIndent()
    )
  }

  private fun fill(db: SupportSQLiteDatabase) {
    fillBooksAndChapters(db)
    fillBookmarks(db)
  }

  private fun fillBookmarks(db: SupportSQLiteDatabase) {
    db.query("SELECT * FROM tableBookmarks").moveToNextLoop {
      val bookmarkPath = getString("bookmarkPath")
      val bookmarkTitle = getString("bookmarkTitle")
      val bookmarkTime = getInt("bookmarkTime")
      db.insert("bookmark") {
        put("file", bookmarkPath)
        put("title", bookmarkTitle)
        put("time", bookmarkTime)
      }
    }
  }

  private fun fillBooksAndChapters(db: SupportSQLiteDatabase) {
    db.query("SELECT * FROM tableBooks").consumeEach { bookCursor ->
      val bookId = bookCursor.getLong("bookId")
      val name = bookCursor.getString("bookName")
      val author = bookCursor.getStringOrNull("bookAuthor")
      val currentMediaPath = bookCursor.getString("bookCurrentMediaPath")
      val playbackSpeed = bookCursor.getFloat("bookSpeed")
      val root = bookCursor.getString("bookRoot")
      val time = bookCursor.getInt("bookTime")
      val type = bookCursor.getString("bookType")
      val active = bookCursor.getInt("BOOK_ACTIVE")
      val loudnessGain = bookCursor.getIntOrNull("loudnessGain") ?: 0

      val newId = UUID.randomUUID().toString()

      db.insert("bookMetaData") {
        put("id", newId)
        put("type", type)
        put("author", author)
        put("name", name)
        put("root", root)
        put("addedAtMillis", 0L)
      }

      db.insert("bookSettings") {
        put("id", newId)
        put("currentFile", currentMediaPath)
        put("positionInChapter", time)
        put("playbackSpeed", playbackSpeed)
        put("loudnessGain", loudnessGain)
        put("skipSilence", 0)
        put("active", active)
        put("lastPlayedAtMillis", 0)
      }

      db.query("SELECT * FROM tableChapters WHERE bookId = $bookId").consumeEach { chapterCursor ->
        val duration = chapterCursor.getInt("chapterDuration")
        val chapterName = chapterCursor.getString("chapterName")
        val chapterPath = chapterCursor.getString("chapterPath")
        val lastModified = chapterCursor.getInt("lastModified")
        val marks = chapterCursor.getStringOrNull("marks") ?: "{}"

        db.insert(
          "chapters",
          SQLiteDatabase.CONFLICT_FAIL,
          contentValuesOf(
            "file" to chapterPath,
            "name" to chapterName,
            "duration" to duration,
            "fileLastModified" to lastModified,
            "marks" to marks,
            "bookId" to newId
          )
        )
      }
    }
  }

  private fun SupportSQLiteDatabase.insert(
    tableName: String,
    contentValues: (ContentValues).() -> Unit
  ) {
    val cv = ContentValues()
    contentValues(cv)
    insert(tableName, SQLiteDatabase.CONFLICT_FAIL, cv)
  }

  private fun deleteOldTables(db: SupportSQLiteDatabase) {
    db.execSQL("DROP TABLE tableBooks")
    db.execSQL("DROP TABLE tableBookmarks")
    db.execSQL("DROP TABLE tableChapters")
  }
}
