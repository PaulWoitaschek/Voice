package voice.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.squareup.anvil.annotations.ContributesMultibinding
import voice.common.AppScope
import voice.data.repo.internals.transaction
import javax.inject.Inject

@ContributesMultibinding(
  scope = AppScope::class,
  boundType = Migration::class,
)
class Migration37to38
@Inject constructor() : IncrementalMigration(37) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.transaction {
      // add new chapter mark table
      db.execSQL("ALTER TABLE tableChapters ADD marks TEXT")

      // invalidate modification time stamps so the chapters will be re-scanned
      val cv = ContentValues().apply {
        put("lastModified", 0)
      }
      db.update("tableChapters", SQLiteDatabase.CONFLICT_FAIL, cv, null, null)
    }
  }
}
