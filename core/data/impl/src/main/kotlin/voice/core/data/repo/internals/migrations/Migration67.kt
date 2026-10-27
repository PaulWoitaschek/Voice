package voice.core.data.repo.internals.migrations

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec

@DeleteTable(tableName = "bookSearchFts")
internal class Migration67 : AutoMigrationSpec
