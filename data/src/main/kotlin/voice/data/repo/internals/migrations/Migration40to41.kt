package voice.data.repo.internals.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import voice.common.AppScope
import javax.inject.Inject

@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
class Migration40to41
@Inject constructor() : IncrementalMigration(40) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE tableBooks ADD loudnessGain INTEGER")
  }
}
