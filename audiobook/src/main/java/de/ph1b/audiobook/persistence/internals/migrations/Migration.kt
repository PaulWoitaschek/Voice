package de.ph1b.audiobook.persistence.internals.migrations

import android.database.sqlite.SQLiteDatabase

/**
 * A database migration
 *
 * @author Paul Woitaschek
 */
interface Migration {
  fun migrate(db: SQLiteDatabase)
}