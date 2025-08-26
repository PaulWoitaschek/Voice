package voice.core.data.repo.internals.migrations

import androidx.room.migration.Migration

public abstract class IncrementalMigration(from: Int) : Migration(from, from + 1)
