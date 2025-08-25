package voice.data.repo.internals.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
@Inject
public class Migration23to24 : IncrementalMigration(23) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("DROP TABLE IF EXISTS TABLE_BOOK")
    db.execSQL("DROP TABLE IF EXISTS TABLE_CHAPTERS")

    db.execSQL(
      """
      CREATE TABLE TABLE_BOOK (
        BOOK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        BOOK_TYPE TEXT NOT NULL,
        BOOK_ROOT TEXT NOT NULL
      )
    """,
    )
    db.execSQL(
      """
      CREATE TABLE TABLE_CHAPTERS (
        CHAPTER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        CHAPTER_PATH TEXT NOT NULL,
        CHAPTER_DURATION INTEGER NOT NULL,
        CHAPTER_NAME TEXT NOT NULL,
        BOOK_ID INTEGER NOT NULL,
        FOREIGN KEY(BOOK_ID) REFERENCES TABLE_BOOK(BOOK_ID)
    )
    """,
    )
  }
}
