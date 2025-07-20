package voice.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import voice.common.AppScope
import javax.inject.Inject

@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
class Migration46
@Inject constructor() : IncrementalMigration(46) {

  override fun migrate(db: SupportSQLiteDatabase) {
    // invalidate modification time stamps so the chapters will be re-scanned
    val lastModifiedCv = ContentValues().apply {
      put("fileLastModified", 0)
    }
    db.update("chapters", SQLiteDatabase.CONFLICT_FAIL, lastModifiedCv, null, null)
  }
}
