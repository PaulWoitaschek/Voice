package voice.core.data.repo.internals.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.data.repo.internals.AppDb
import voice.core.data.repo.internals.allMigrations
import voice.core.data.repo.internals.getString
import voice.core.data.repo.internals.getStringOrNull
import voice.core.data.repo.internals.mapRows
import kotlin.test.Test
import kotlin.test.assertEquals

private const val DB_NAME = "testDb"

@RunWith(AndroidJUnit4::class)
class Migration67to68Test {

  @Rule
  @JvmField
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    AppDb::class.java,
  )

  @Test
  fun `books keep the author from their files`() {
    helper.createDatabase(DB_NAME, 67).use {
      it.insert(book(id = "content://book", name = "Dune", author = "Frank Herbert"))
    }
    val migrated = helper.runMigrationsAndValidate(DB_NAME, 68, true, *allMigrations())

    val rows = migrated.query("SELECT author, customAuthor FROM content2")
      .mapRows { listOf(getString("author"), getStringOrNull("customAuthor")) }
    assertEquals(expected = listOf(listOf("Frank Herbert", null)), actual = rows)
  }

  @Test
  fun `books can still be written after the search table is gone`() {
    helper.createDatabase(DB_NAME, 67).use {
      it.insert(book(id = "content://dune", name = "Dune", author = "Frank Herbert"))
    }
    // opened like the app does, as the migration test helper leaves the triggers of the search table in place
    val appDb = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java, DB_NAME)
      .addMigrations(*allMigrations())
      .allowMainThreadQueries()
      .build()
    val db = appDb.openHelper.writableDatabase

    db.update("content2", SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply { put("name", "Dune Messiah") }, null, null)
    db.insert("content2", SQLiteDatabase.CONFLICT_REPLACE, book(id = "content://dune", name = "Children of Dune", author = "Frank Herbert"))
    db.insert(book(id = "content://hyperion", name = "Hyperion", author = "Dan Simmons"))
    db.delete("content2", "id = ?", arrayOf("content://hyperion"))

    assertEquals(
      expected = listOf("Children of Dune"),
      actual = db.query("SELECT name FROM content2").mapRows { getString("name") },
    )
    assertEquals(
      expected = emptyList(),
      actual = db.query("SELECT name FROM sqlite_master WHERE name LIKE '%bookSearchFts%'").mapRows { getString("name") },
    )
    appDb.close()
  }

  private fun SupportSQLiteDatabase.insert(values: ContentValues) {
    insert("content2", SQLiteDatabase.CONFLICT_FAIL, values)
  }

  private fun book(
    id: String,
    name: String,
    author: String,
  ) = ContentValues().apply {
    put("id", id)
    put("playbackSpeed", 1F)
    put("skipSilence", 0)
    put("isActive", 1)
    put("lastPlayedAt", "1970-01-01T00:00:00Z")
    put("name", name)
    put("author", author)
    put("addedAt", "1970-01-01T00:00:00Z")
    put("chapters", "[\"$id/1.mp3\"]")
    put("currentChapter", "$id/1.mp3")
    put("positionInChapter", 0L)
    put("gain", 0F)
  }
}
