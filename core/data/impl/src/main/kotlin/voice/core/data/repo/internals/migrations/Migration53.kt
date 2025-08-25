package voice.core.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import java.time.Instant

// clear the fileLastModified to trigger a rescan of the chapters
@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
@Inject
public class Migration53 : IncrementalMigration(53) {

  override fun migrate(db: SupportSQLiteDatabase) {
    val lastModifiedCv = ContentValues().apply {
      put("fileLastModified", Instant.EPOCH.toString())
    }
    db.update(
      "chapters2",
      SQLiteDatabase.CONFLICT_FAIL,
      lastModifiedCv,
      null,
      null,
    )
  }
}
