package voice.core.data.repo.internals.migrations

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import voice.core.data.AUDIOBOOKSHELF_SCHEME

internal class Migration66 : AutoMigrationSpec {

  override fun onPostMigrate(db: SupportSQLiteDatabase) {
    // a rename can't be told apart from a name from the files, so the next scan drops the ones its files give
    db.execSQL(
      "UPDATE content2 SET customName = name WHERE id NOT LIKE ?",
      arrayOf("$AUDIOBOOKSHELF_SCHEME:%"),
    )
    // the recreated search table starts out empty
    db.execSQL("INSERT INTO bookSearchFts(bookSearchFts) VALUES('rebuild')")
  }
}
