package voice.core.data.repo.internals.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.data.repo.internals.AppDb
import voice.core.data.repo.internals.allMigrations
import voice.core.data.repo.internals.getString
import voice.core.data.repo.internals.getStringOrNull
import voice.core.data.repo.internals.mapRows

@RunWith(AndroidJUnit4::class)
class Migration69to70Test {

  @Rule
  @JvmField
  val helper =
    MigrationTestHelper(
      InstrumentationRegistry.getInstrumentation(),
      AppDb::class.java,
    )

  @Test
  fun `books stay, with no library until the next sync`() {
    val dbName = "testDb"
    helper.createDatabase(dbName, 69).use { db ->
      db.insert(
        "content2",
        SQLiteDatabase.CONFLICT_FAIL,
        ContentValues().apply {
          put("id", "abs://item/dune")
          put("playbackSpeed", 1F)
          put("skipSilence", 0)
          put("isActive", 1)
          put("lastPlayedAt", "1970-01-01T00:00:00Z")
          put("name", "Dune")
          put("addedAt", "1970-01-01T00:00:00Z")
          put("chapters", "[\"abs://item/dune/track/1/ino1\"]")
          put("currentChapter", "abs://item/dune/track/1/ino1")
          put("positionInChapter", 0L)
          put("gain", 0F)
        },
      )
    }

    val migrated = helper.runMigrationsAndValidate(dbName, 70, true, *allMigrations())

    assertEquals(
      expected = listOf(listOf("abs://item/dune", "Dune", null)),
      actual =
        migrated.query("SELECT * FROM content2").mapRows {
          listOf(getString("id"), getString("name"), getStringOrNull("serverLibraryId"))
        },
    )
  }
}
