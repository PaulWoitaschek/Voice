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
public class Migration40to41 : IncrementalMigration(40) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE tableBooks ADD loudnessGain INTEGER")
  }
}
