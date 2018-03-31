package de.ph1b.audiobook.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.database.sqlite.transaction

class Migration37to38 : Migration {
  override fun migrate(db: SQLiteDatabase) {
    db.transaction {
      // add new chapter mark table
      db.execSQL("ALTER TABLE tableChapters ADD marks TEXT")

      // invalidate modification time stamps so the chapters will be re-scanned
      val cv = ContentValues().apply {
        put("lastModified", 0)
      }
      db.update("tableChapters", cv, null, null)
    }
  }
}
