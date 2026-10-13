package voice.core.data.repo.internals.migrations

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn(tableName = "bookmark2", columnName = "setBySleepTimer")
internal class Migration62 : AutoMigrationSpec
