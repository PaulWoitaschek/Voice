package de.ph1b.audiobook.persistence.internals.migrations

import android.database.sqlite.SQLiteDatabase

/**
 * A database migration
 */
interface Migration {
  fun migrate(db: SQLiteDatabase)
}
