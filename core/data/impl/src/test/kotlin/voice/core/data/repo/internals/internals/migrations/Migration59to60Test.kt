package voice.core.data.repo.internals.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.data.repo.internals.AppDb
import voice.core.data.repo.internals.allMigrations
import voice.core.data.repo.internals.getString
import voice.core.data.repo.internals.mapRows
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class Migration59to60Test {

  @Rule
  @JvmField
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    AppDb::class.java,
  )

  @Test
  fun `existing bookmarks become notes and the history starts empty`() {
    val dbName = "testDb"
    helper.createDatabase(dbName, 59).use { db ->
      db.insert(
        "bookmark2",
        SQLiteDatabase.CONFLICT_FAIL,
        ContentValues().apply {
          put("bookId", "book")
          put("chapterId", "chapter")
          put("title", "Mira")
          put("time", 1000L)
          put("addedAt", "2026-10-07T08:00:00Z")
          put("setBySleepTimer", 1)
          put("id", "0123456789abcdef0123456789abcdef")
        },
      )
    }

    val migrated = helper.runMigrationsAndValidate(dbName, 60, true, *allMigrations())

    val kinds = migrated.query("SELECT kind FROM bookmark2").mapRows { getString("kind") }
    assertEquals(expected = listOf("Note"), actual = kinds)
    val events = migrated.query("SELECT * FROM listening_event").mapRows { getString("bookId") }
    assertEquals(expected = emptyList(), actual = events)
  }
}
