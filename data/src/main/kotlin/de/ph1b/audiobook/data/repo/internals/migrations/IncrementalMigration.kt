package de.ph1b.audiobook.data.repo.internals.migrations

import androidx.room.migration.Migration

abstract class IncrementalMigration(from: Int) : Migration(from, from + 1)
