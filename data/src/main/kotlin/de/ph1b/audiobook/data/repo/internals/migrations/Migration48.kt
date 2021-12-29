package de.ph1b.audiobook.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration48 : IncrementalMigration(48) {

  override fun migrate(db: SupportSQLiteDatabase) {
    // there was a bug a in the chapter parsing, trigger a scan.
    val lastModifiedCv = ContentValues().apply {
      put("fileLastModified", 0)
    }
    db.update("chapters", SQLiteDatabase.CONFLICT_FAIL, lastModifiedCv, null, null)
  }
}
