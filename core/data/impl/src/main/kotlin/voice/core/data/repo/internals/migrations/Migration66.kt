package voice.core.data.repo.internals.migrations

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import voice.core.data.AUDIOBOOKSHELF_SCHEME

internal class Migration66 : AutoMigrationSpec {

  override fun onPostMigrate(db: SupportSQLiteDatabase) {
    // A name the listener gave can't be told apart from one from the files yet. Both count as the listener's, and the
    // next scan reads the files again and drops the names it finds in them.
    db.execSQL(
      "UPDATE content2 SET customName = name WHERE id NOT LIKE ?",
      arrayOf("$AUDIOBOOKSHELF_SCHEME:%"),
    )
    // the recreated search table starts out empty
    db.execSQL("INSERT INTO bookSearchFts(bookSearchFts) VALUES('rebuild')")
  }
}
