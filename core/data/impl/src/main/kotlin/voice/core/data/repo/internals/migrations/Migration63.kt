package voice.core.data.repo.internals.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding

// The history shows stretches of listening, and no longer records settings and bookmarks. Their types are gone, so
// their rows would fail to load.
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
