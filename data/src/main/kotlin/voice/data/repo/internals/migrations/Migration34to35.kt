package voice.data.repo.internals.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

private const val TABLE_NAME = "tableBooks"

@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
@Inject
public class Migration34to35 : IncrementalMigration(34) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.delete(TABLE_NAME, "bookId<=", arrayOf(-1))
  }
}
