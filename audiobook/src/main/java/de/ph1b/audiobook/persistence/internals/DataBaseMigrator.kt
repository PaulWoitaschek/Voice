package de.ph1b.audiobook.persistence.internals

import android.database.sqlite.SQLiteDatabase
import de.ph1b.audiobook.persistence.internals.migrations.Migration23to24
import de.ph1b.audiobook.persistence.internals.migrations.Migration24to25
import de.ph1b.audiobook.persistence.internals.migrations.Migration25to26
import de.ph1b.audiobook.persistence.internals.migrations.Migration26to27
import de.ph1b.audiobook.persistence.internals.migrations.Migration27to28
import de.ph1b.audiobook.persistence.internals.migrations.Migration28to29
import de.ph1b.audiobook.persistence.internals.migrations.Migration29to30
import de.ph1b.audiobook.persistence.internals.migrations.Migration30to31
import de.ph1b.audiobook.persistence.internals.migrations.Migration31to32
import de.ph1b.audiobook.persistence.internals.migrations.Migration32to34
import de.ph1b.audiobook.persistence.internals.migrations.Migration34to35
import de.ph1b.audiobook.persistence.internals.migrations.Migration35to36
import de.ph1b.audiobook.persistence.internals.migrations.Migration36to37
import de.ph1b.audiobook.persistence.internals.migrations.Migration37to38
import de.ph1b.audiobook.persistence.internals.migrations.Migration38to39
import de.ph1b.audiobook.persistence.internals.migrations.Migration39to40
import de.ph1b.audiobook.persistence.internals.migrations.Migration40to41
import de.ph1b.audiobook.persistence.internals.migrations.Migration41to42
import de.ph1b.audiobook.persistence.internals.migrations.Migration42to43
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
