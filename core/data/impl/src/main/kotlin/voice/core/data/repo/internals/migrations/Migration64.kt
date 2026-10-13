package voice.core.data.repo.internals.migrations

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn(tableName = "listening_event", columnName = "value")
@DeleteColumn(tableName = "listening_event", columnName = "sourcePackage")
@DeleteColumn(tableName = "listening_event", columnName = "bookmarkId")
@DeleteColumn(tableName = "listening_event", columnName = "bookmarkKind")
@DeleteColumn(tableName = "listening_event", columnName = "bookmarkAddedAtMillis")
internal class Migration64 : AutoMigrationSpec
