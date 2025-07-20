package voice.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import voice.common.AppScope
import javax.inject.Inject

@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
class Migration47
@Inject constructor() : IncrementalMigration(47) {

  override fun migrate(db: SupportSQLiteDatabase) {
    // the format of the marks has changed. Write an empty array. Also clear the fileLastModified to trigger a rescan.
    val lastModifiedCv = ContentValues().apply {
      put("marks", "[]")
      put("fileLastModified", 0)
    }
    db.update("chapters", SQLiteDatabase.CONFLICT_FAIL, lastModifiedCv, null, null)
  }
}
