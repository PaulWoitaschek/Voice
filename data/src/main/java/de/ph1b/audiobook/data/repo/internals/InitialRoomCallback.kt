package de.ph1b.audiobook.data.repo.internals

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.RoomDatabase
import de.ph1b.audiobook.data.repo.internals.tables.BookTable
import javax.inject.Inject

class InitialRoomCallback @Inject constructor() : RoomDatabase.Callback() {

  override fun onCreate(db: SupportSQLiteDatabase) {
    db.execSQL(BookTable.CREATE_TABLE)
  }
}
