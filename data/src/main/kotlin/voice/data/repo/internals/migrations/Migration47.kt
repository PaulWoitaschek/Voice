package voice.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.squareup.anvil.annotations.ContributesMultibinding
import voice.common.AppScope
import javax.inject.Inject

@ContributesMultibinding(
  scope = AppScope::class,
  boundType = Migration::class,
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
