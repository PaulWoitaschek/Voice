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
import voice.core.data.repo.internals.getLong
import voice.core.data.repo.internals.getString
import voice.core.data.repo.internals.getStringOrNull
import voice.core.data.repo.internals.mapRows

@RunWith(AndroidJUnit4::class)
class Migration70to71Test {

  @Rule
  @JvmField
  val helper =
    MigrationTestHelper(
      InstrumentationRegistry.getInstrumentation(),
      AppDb::class.java,
    )

  @Test
  fun `books stay, with no mark as completed and an empty tally`() {
    val dbName = "testDb"
    helper.createDatabase(dbName, 70).use { db ->
      db.insert(
        "content2",
        SQLiteDatabase.CONFLICT_FAIL,
        ContentValues().apply {
          put("id", "dune")
          put("playbackSpeed", 1F)
          put("skipSilence", 0)
          put("isActive", 1)
          put("lastPlayedAt", "1970-01-01T00:00:00Z")
          put("name", "Dune")
          put("addedAt", "1970-01-01T00:00:00Z")
          put("chapters", "[\"dune/1.mp3\"]")
          put("currentChapter", "dune/1.mp3")
          put("positionInChapter", 1000L)
          put("gain", 0F)
        },
      )
    }

    val migrated = helper.runMigrationsAndValidate(dbName, 71, true, *allMigrations())

    assertEquals(
      expected = listOf(listOf("dune", null, 0L)),
      actual =
        migrated.query("SELECT * FROM content2").mapRows {
          listOf(
            getString("id"),
            getStringOrNull("markedCompletedAt"),
            getLong("startedBeforeMark"),
          )
        },
    )
    assertEquals(
      expected = 0,
      actual = migrated.query("SELECT * FROM listening_tally").use { it.count },
    )
  }
}
