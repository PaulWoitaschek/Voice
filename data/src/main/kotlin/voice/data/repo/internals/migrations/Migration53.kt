package voice.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.squareup.anvil.annotations.ContributesMultibinding
import voice.common.AppScope
import java.time.Instant
import javax.inject.Inject

// clear the fileLastModified to trigger a rescan of the chapters
@ContributesMultibinding(
  scope = AppScope::class,
  boundType = Migration::class,
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
