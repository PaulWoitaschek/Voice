package voice.core.data.repo.internals.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.provider.DocumentsContract
import androidx.room.testing.MigrationTestHelper
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

@RunWith(AndroidJUnit4::class)
class Migration66to67Test {

  @Rule
  @JvmField
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    AppDb::class.java,
  )

  @Test
  fun `chapters take their file name from a document that holds the path`() {
    val inFolder = DocumentsContract.buildDocumentUriUsingTree(
      DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", "primary:Audiobooks"),
      "primary:Audiobooks/Book: Part 1/01 - Intro.mp3",
    ).toString()
    val atTheRoot = "content://com.android.externalstorage.documents/document/primary%3ABook.m4b"
    val file = "file:///storage/emulated/0/Audiobooks/Book/02.mp3"
    val withoutPath = "content://com.android.providers.media.documents/document/audio%3A42"
    val dbName = "testDb"
    helper.createDatabase(dbName, 66).use { db ->
      listOf(inFolder, atTheRoot, file, withoutPath).forEach {
        db.insert("chapters2", SQLiteDatabase.CONFLICT_FAIL, chapter(it))
      }
    }

    val migrated = helper.runMigrationsAndValidate(dbName, 67, true, *allMigrations())

    val fileNames = migrated.query("SELECT id, fileName FROM chapters2")
      .mapRows { getString("id") to getStringOrNull("fileName") }
      .toMap()
    assertEquals(
      expected = mapOf(
        inFolder to "01 - Intro.mp3",
        atTheRoot to "Book.m4b",
        file to "02.mp3",
        withoutPath to null,
      ),
      actual = fileNames,
    )
  }

  private fun chapter(id: String) = ContentValues().apply {
    put("id", id)
    put("duration", 1000L)
    put("fileLastModified", "1970-01-01T00:00:00Z")
    put("fileSize", 10L)
    put("markData", "[]")
  }
}
