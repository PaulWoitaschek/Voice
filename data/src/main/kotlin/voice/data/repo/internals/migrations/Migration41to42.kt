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
class Migration41to42 : IncrementalMigration(41) {
  override fun migrate(db: SupportSQLiteDatabase) {
    // invalidate modification time stamps so the chapters will be re-scanned
    val lastModifiedCv = ContentValues().apply {
      put("lastModified", 0)
    }
    db.update("tableChapters", SQLiteDatabase.CONFLICT_FAIL, lastModifiedCv, null, null)

    val marksCv = ContentValues().apply {
      put("marks", null as String?)
    }
    db.update("tableChapters", SQLiteDatabase.CONFLICT_FAIL, marksCv, null, null)
  }
}
