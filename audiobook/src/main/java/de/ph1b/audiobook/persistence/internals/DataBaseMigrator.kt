package de.ph1b.audiobook.persistence.internals

import android.database.sqlite.SQLiteDatabase
import de.ph1b.audiobook.persistence.internals.migrations.*
import i


class DataBaseMigrator(private val db: SQLiteDatabase) {

  private fun migration(fromVersion: Int) = when (fromVersion) {
    23 -> Migration23()
    24 -> Migration24()
    25 -> Migration25()
    26 -> Migration26()
    27 -> Migration27()
    28 -> Migration28()
    29 -> Migration29()
    30 -> Migration30()
    31 -> Migration31()
    32 -> Migration32()
    34 -> Migration34()
    35 -> Migration35()
    36 -> Migration36()
    37 -> Migration37()
    38 -> Migration38()
    else -> null
  }

  fun upgrade(newVersion: Int) {
    for (from in 1..newVersion) {
      val migration = migration(from)
      if (migration != null) {
        i { "upgrade fromVersion=$from" }
        migration.migrate(db)
      }
    }
  }
}
