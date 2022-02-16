package voice.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration50 : IncrementalMigration(50) {

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
