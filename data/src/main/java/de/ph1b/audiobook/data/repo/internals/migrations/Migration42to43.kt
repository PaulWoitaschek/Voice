package de.ph1b.audiobook.data.repo.internals.migrations

import android.content.ContentValues
import androidx.room.OnConflictStrategy
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration42to43 : IncrementalMigration(42) {
  override fun migrate(db: SupportSQLiteDatabase) {
    // invalidate modification time stamps so the chapters will be re-scanned
    val lastModifiedCv = ContentValues().apply {
      put("lastModified", 0)
    }
    db.update("tableChapters", OnConflictStrategy.FAIL, lastModifiedCv, null, null)
  }
}
