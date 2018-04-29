package de.ph1b.audiobook.data.repo.internals.migrations

import android.arch.persistence.db.SupportSQLiteDatabase

/**
 * In 41 the loudness column was introduced.
 */
class Migration40to41 : IncrementalMigration(40) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE tableBooks ADD loudnessGain INTEGER")
  }
}
