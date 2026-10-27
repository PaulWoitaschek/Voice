package voice.core.data.repo.internals.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.data.repo.internals.AppDb
import voice.core.data.repo.internals.allMigrations
import voice.core.data.repo.internals.getLong
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
  fun `the edits move to their own table`() {
    helper.createDatabase(DB_NAME, 67).use { db ->
      db.insert(book(id = "content://dune", name = "Dune", author = "Herbert"))
      db.insert(
        book(id = "content://hyperion", name = "Hyperion", author = "Dan Simmons").apply {
          put("customName", "Hyperion Cantos")
          put("customCover", "/covers/hyperion.webp")
        },
      )
      db.insert(book(id = "content://solaris", name = "Solaris", author = "Lem").apply { put("customCover", "/covers/solaris.webp") })
    }

    val migrated = helper.runMigrationsAndValidate(DB_NAME, 68, true, *allMigrations())

    assertEquals(
      expected = listOf(
        listOf("content://hyperion", "Hyperion Cantos", null, "/covers/hyperion.webp"),
        listOf("content://solaris", null, null, "/covers/solaris.webp"),
      ),
      actual = migrated.query("SELECT * FROM customMetadata ORDER BY bookId").mapRows {
        listOf(getString("bookId"), getStringOrNull("name"), getStringOrNull("author"), getStringOrNull("cover"))
      },
    )
    assertEquals(
      expected = listOf(
        listOf("content://dune", "Dune", "Herbert", 1234L),
        listOf("content://hyperion", "Hyperion", "Dan Simmons", 1234L),
        listOf("content://solaris", "Solaris", "Lem", 1234L),
      ),
      actual = migrated.query("SELECT * FROM content2 ORDER BY id").mapRows {
        listOf(getString("id"), getString("name"), getStringOrNull("author"), getLong("positionInChapter"))
      },
    )
  }

  @Test
  fun `the search table is gone and books can still be written`() {
    helper.createDatabase(DB_NAME, 67).use { it.insert(book(id = "content://dune", name = "Dune", author = "Herbert")) }

    val migrated = helper.runMigrationsAndValidate(DB_NAME, 68, true, *allMigrations())
    migrated.update("content2", SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply { put("name", "Dune Messiah") }, null, null)
    migrated.insert(book(id = "content://hyperion", name = "Hyperion", author = "Simmons"))

    assertEquals(
      expected = emptyList(),
      actual = migrated.query("SELECT name FROM sqlite_master WHERE name LIKE '%bookSearchFts%'").mapRows { getString("name") },
    )
    assertEquals(
      expected = listOf("Dune Messiah", "Hyperion"),
      actual = migrated.query("SELECT name FROM content2 ORDER BY id").mapRows { getString("name") },
    )
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
    put("positionInChapter", 1234L)
    put("gain", 0F)
  }
}
