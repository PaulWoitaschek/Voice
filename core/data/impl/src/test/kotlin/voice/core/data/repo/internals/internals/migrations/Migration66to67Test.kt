package voice.core.data.repo.internals.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.repo.internals.AppDb
import voice.core.data.repo.internals.allMigrations
import voice.core.data.repo.internals.getString
import voice.core.data.repo.internals.getStringOrNull
import voice.core.data.repo.internals.mapRows
import kotlin.test.Test
import kotlin.test.assertEquals

private const val DB_NAME = "testDb"

@RunWith(AndroidJUnit4::class)
class Migration66to67Test {

  @Rule
  @JvmField
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    AppDb::class.java,
  )

  @Test
  fun `names of books on the device count as the listener's until a scan compares them with the files`() {
    val migrated = migrate {
      insert(book(id = "content://book", name = "Dune"))
      insert(book(id = "abs://item/1", name = "Server Book"))
    }

    val rows = migrated.query("SELECT id, name, customName, metadataFileLastModified FROM content2")
      .mapRows {
        listOf(getString("id"), getString("name"), getStringOrNull("customName"), getStringOrNull("metadataFileLastModified"))
      }
    assertEquals(
      expected = listOf(
        listOf("abs://item/1", "Server Book", null, null),
        listOf("content://book", "Dune", "Dune", null),
      ),
      actual = rows.sortedBy { it.first() },
    )
  }

  @Test
  fun `books are found by their names after the migration`() = runTest {
    helper.createDatabase(DB_NAME, 66).use {
      it.insert(book(id = "content://book", name = "Dune"))
    }
    // opened like the app does, which sets up the search again after migrating
    val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java, DB_NAME)
      .addMigrations(*allMigrations())
      .allowMainThreadQueries()
      .build()
    db.openHelper.writableDatabase.insert(
      book(id = "content://other", name = "Track 01").apply { put("customName", "Hyperion") },
    )
    val dao = db.bookContentDao()

    assertEquals(expected = listOf(BookId("content://book")), actual = dao.search("dune*"))
    assertEquals(expected = listOf(BookId("content://other")), actual = dao.search("hyperion*"))
    assertEquals(expected = listOf(BookId("content://other")), actual = dao.search("track*"))
    db.close()
  }

  private fun migrate(atVersion66: SupportSQLiteDatabase.() -> Unit): SupportSQLiteDatabase {
    helper.createDatabase(DB_NAME, 66).use(atVersion66)
    return helper.runMigrationsAndValidate(DB_NAME, 67, true, *allMigrations())
  }

  private fun SupportSQLiteDatabase.insert(values: ContentValues) {
    insert("content2", SQLiteDatabase.CONFLICT_FAIL, values)
  }

  private fun book(
    id: String,
    name: String,
  ) = ContentValues().apply {
    put("id", id)
    put("playbackSpeed", 1F)
    put("skipSilence", 0)
    put("isActive", 1)
    put("lastPlayedAt", "1970-01-01T00:00:00Z")
    put("name", name)
    put("addedAt", "1970-01-01T00:00:00Z")
    put("chapters", "[\"$id/1.mp3\"]")
    put("currentChapter", "$id/1.mp3")
    put("positionInChapter", 0L)
    put("gain", 0F)
  }
}
