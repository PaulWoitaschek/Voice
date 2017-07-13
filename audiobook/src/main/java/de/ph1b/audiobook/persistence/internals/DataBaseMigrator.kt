package de.ph1b.audiobook.persistence.internals

import android.database.sqlite.SQLiteDatabase
import de.ph1b.audiobook.persistence.internals.migrations.*
import i


class DataBaseMigrator(private val db: SQLiteDatabase) {

  private fun migration(fromVersion: Int) = when (fromVersion) {
    23 -> Migration23to24()
    24 -> Migration24to25()
    25 -> Migration25to26()
    26 -> Migration26to27()
    27 -> Migration27to28()
    28 -> Migration28to29()
    29 -> Migration29to30()
    30 -> Migration30to31()
    31 -> Migration31to32()
    32 -> Migration32to34()
    34 -> Migration34to35()
    35 -> Migration35to36()
    36 -> Migration36to37()
    37 -> Migration37to38()
    38 -> Migration38to39()
    39 -> Migration39to40()
    40 -> Migration40to41()
    41 -> Migration41to42()
    42 -> Migration42to43()
    else -> null
  }

  fun upgrade(oldVersion: Int, newVersion: Int) {
    for (from in oldVersion..newVersion) {
      val migration = migration(from)
      if (migration != null) {
        i { "upgrade fromVersion=$from" }
        migration.migrate(db)
      }
    }
  }
}
