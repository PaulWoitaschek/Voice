package voice.core.data.repo.internals.internals.migrations

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.data.repo.internals.AppDb
import voice.core.data.repo.internals.allMigrations
import voice.core.data.repo.internals.createAppDb

@RunWith(AndroidJUnit4::class)
class OldDatabaseTest {

  @Rule
  @JvmField
  val helper =
    MigrationTestHelper(
      InstrumentationRegistry.getInstrumentation(),
      AppDb::class.java,
    )

  @Test
  fun `the oldest supported database migrates to the current schema`() {
    val dbName = "testDb"
    helper.createDatabase(dbName, AppDb.OLDEST_MIGRATED_VERSION).close()

    helper.runMigrationsAndValidate(dbName, AppDb.VERSION, true, *allMigrations()).close()
  }

  @Test
  fun `an older database starts empty`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbName = "oldDb"
    context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null).use { db ->
      db.execSQL("CREATE TABLE content2 (id TEXT NOT NULL PRIMARY KEY)")
      db.execSQL("INSERT INTO content2 (id) VALUES ('content://hyperion')")
      db.version = AppDb.OLDEST_MIGRATED_VERSION - 1
    }

    val appDb = createAppDb(context, dbName, allMigrations().toSet())

    assertEquals(emptyList(), appDb.bookContentDao().all())
    appDb.close()
  }
}
