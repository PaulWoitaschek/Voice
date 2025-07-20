package voice.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import voice.common.AppScope
import java.time.Instant
import javax.inject.Inject

// clear the fileLastModified to trigger a rescan of the chapters
@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
class Migration53
@Inject constructor() : IncrementalMigration(53) {

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
