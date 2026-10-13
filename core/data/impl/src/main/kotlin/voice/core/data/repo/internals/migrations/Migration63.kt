package voice.core.data.repo.internals.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding

// the types of these rows are gone, so they would fail to load
@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
public class Migration63 : IncrementalMigration(63) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL(
      """
      DELETE FROM listening_event
      WHERE type IN ('SpeedChanged', 'SkipSilenceChanged', 'VolumeBoostChanged', 'BookmarkAdded', 'BookmarkDeleted')
      """.trimIndent(),
    )
  }
}
