package de.ph1b.audiobook.data.repo.internals.migrations

import android.arch.persistence.db.SupportSQLiteDatabase

class Migration44to45 : IncrementalMigration(44) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE tableBooks ADD skipSilence INTEGER")
  }
}
