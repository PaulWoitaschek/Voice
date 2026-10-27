package voice.core.data.repo.internals.migrations

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

internal class Migration67 : AutoMigrationSpec {

  override fun onPostMigrate(db: SupportSQLiteDatabase) {
    // the recreated search table starts out empty
    db.execSQL("INSERT INTO bookSearchFts(bookSearchFts) VALUES('rebuild')")
  }
}
