package voice.data.repo.internals.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import voice.common.AppScope
import javax.inject.Inject

private const val TABLE_NAME = "tableBooks"

@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
class Migration34to35
@Inject constructor() : IncrementalMigration(34) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.delete(TABLE_NAME, "bookId<=", arrayOf(-1))
  }
}
