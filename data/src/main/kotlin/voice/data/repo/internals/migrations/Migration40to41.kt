package voice.data.repo.internals.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * In 41 the loudness column was introduced.
 */
class Migration40to41 : IncrementalMigration(40) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE tableBooks ADD loudnessGain INTEGER")
  }
}
