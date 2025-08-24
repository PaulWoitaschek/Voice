package voice.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
@Inject
public class Migration50 : IncrementalMigration(50) {

  override fun migrate(db: SupportSQLiteDatabase) {
    with(db) {
      // there was a bug a in the chapter parsing, trigger a scan.
      val lastModifiedCv = ContentValues().apply {
        put("fileLastModified", 0)
      }
      update("chapters", SQLiteDatabase.CONFLICT_FAIL, lastModifiedCv, null, null)
    }
  }
}
