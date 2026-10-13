package voice.core.data.repo.internals.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import voice.core.data.MAX_CHAPTER_IDS_BYTES

// Reading a book whose chapter ids don't fit into the cursor window crashes, and the library is read on every start.
@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
public class Migration65 : IncrementalMigration(65) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("DELETE FROM content2 WHERE length(CAST(chapters AS BLOB)) > $MAX_CHAPTER_IDS_BYTES")
  }
}
