package voice.core.data.repo.internals.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.data.MAX_CHAPTER_IDS_BYTES
import voice.core.data.repo.internals.AppDb
import voice.core.data.repo.internals.allMigrations
import voice.core.data.repo.internals.getString
import voice.core.data.repo.internals.mapRows
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class Migration65to66Test {

  @Rule
  @JvmField
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    AppDb::class.java,
  )

  @Test
  fun `books with more chapters than fit into one book are removed`() {
    val dbName = "testDb"
    helper.createDatabase(dbName, 65).use { db ->
      db.insert("content2", SQLiteDatabase.CONFLICT_FAIL, book(id = "small", chapterId = "chapter"))
      db.insert("content2", SQLiteDatabase.CONFLICT_FAIL, book(id = "atTheLimit", chapterId = "a".repeat(MAX_CHAPTER_IDS_BYTES - 4)))
      db.insert("content2", SQLiteDatabase.CONFLICT_FAIL, book(id = "tooBig", chapterId = "a".repeat(MAX_CHAPTER_IDS_BYTES - 3)))
    }

    val migrated = helper.runMigrationsAndValidate(dbName, 66, true, *allMigrations())

    val ids = migrated.query("SELECT id FROM content2").mapRows { getString("id") }
    assertEquals(expected = listOf("atTheLimit", "small"), actual = ids.sorted())
  }

  private fun book(
    id: String,
    chapterId: String,
  ) = ContentValues().apply {
    put("id", id)
    put("playbackSpeed", 1F)
    put("skipSilence", 0)
    put("isActive", 1)
    put("lastPlayedAt", "1970-01-01T00:00:00Z")
    put("name", id)
    put("addedAt", "1970-01-01T00:00:00Z")
    put("chapters", "[\"$chapterId\"]")
    put("currentChapter", chapterId)
    put("positionInChapter", 0L)
    put("gain", 0F)
  }
}
