package de.ph1b.audiobook.persistence.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import de.ph1b.audiobook.persistence.internals.asTransaction

class Migration37to38 : Migration {
  override fun migrate(db: SQLiteDatabase) {
    db.asTransaction {
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