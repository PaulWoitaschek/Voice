package de.ph1b.audiobook.data.repo.internals.migrations

import android.arch.persistence.room.migration.Migration

abstract class IncrementalMigration(from: Int) : Migration(from, from + 1)
