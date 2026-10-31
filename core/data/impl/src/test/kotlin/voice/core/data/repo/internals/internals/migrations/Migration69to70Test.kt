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
import voice.core.data.repo.internals.getLong
import voice.core.data.repo.internals.getString
import voice.core.data.repo.internals.getStringOrNull
import voice.core.data.repo.internals.mapRows
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class Migration69to70Test {

  @Rule
  @JvmField
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    AppDb::class.java,
  )

  @Test
  fun `the edits stay and no book is hidden`() {
    val dbName = "testDb"
    helper.createDatabase(dbName, 69).use { db ->
      db.insert(
        "customMetadata",
        SQLiteDatabase.CONFLICT_FAIL,
        ContentValues().apply {
          put("bookId", "content://hyperion")
          put("name", "Hyperion Cantos")
          put("series", "Hyperion")
        },
      )
    }

    val migrated = helper.runMigrationsAndValidate(dbName, 70, true, *allMigrations())

    assertEquals(
      expected = listOf(listOf("content://hyperion", "Hyperion Cantos", "Hyperion", 0L)),
      actual = migrated.query("SELECT * FROM customMetadata").mapRows {
        listOf(getString("bookId"), getStringOrNull("name"), getStringOrNull("series"), getLong("hidden"))
      },
    )
  }
}
