package voice.core.data.repo.internals.migrations

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec

@DeleteTable(tableName = "bookmark")
@DeleteTable(tableName = "chapters")
@DeleteTable(tableName = "bookMetaData")
@DeleteTable(tableName = "bookSettings")
internal class Migration56 : AutoMigrationSpec
