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
class Migration63to65Test {

  @Rule
  @JvmField
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    AppDb::class.java,
  )

  @Test
  fun `the history keeps listening and drops settings and bookmarks`() {
    val dbName = "testDb"
    val types = listOf("Play", "SpeedChanged", "SkipSilenceChanged", "VolumeBoostChanged", "BookmarkAdded", "BookmarkDeleted", "Pause")
    helper.createDatabase(dbName, 63).use { db ->
      types.forEachIndexed { index, type ->
        db.insert(
          "listening_event",
          SQLiteDatabase.CONFLICT_FAIL,
          ContentValues().apply {
            put("bookId", "book")
            put("type", type)
            put("source", "App")
            put("atMillis", index.toLong())
            put("chapterId", "chapter")
            put("time", 0L)
            put("value", "1.5")
          },
        )
      }
    }

    val migrated = helper.runMigrationsAndValidate(dbName, 65, true, *allMigrations())

    val left = migrated.query("SELECT type FROM listening_event ORDER BY atMillis").mapRows { getString("type") }
    assertEquals(expected = listOf("Play", "Pause"), actual = left)
  }
}
