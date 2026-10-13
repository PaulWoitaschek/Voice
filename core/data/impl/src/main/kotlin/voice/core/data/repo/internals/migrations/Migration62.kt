package voice.core.data.repo.internals.migrations

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn(tableName = "bookmark2", columnName = "setBySleepTimer")
@DeleteColumn(tableName = "listening_event", columnName = "bookmarkSetBySleepTimer")
internal class Migration62 : AutoMigrationSpec
