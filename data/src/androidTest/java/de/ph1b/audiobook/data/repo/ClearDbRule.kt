package de.ph1b.audiobook.data.repo

import android.support.test.InstrumentationRegistry
import de.ph1b.audiobook.data.repo.internals.InternalDb
import org.junit.rules.ExternalResource


class ClearDbRule : ExternalResource() {

  override fun before() {
    super.before()
    clearDb()
  }

  override fun after() {
    super.after()
    clearDb()
  }

  private fun clearDb() {
    val context = InstrumentationRegistry.getTargetContext()
    val dbName = InternalDb.DATABASE_NAME
    context.deleteDatabase(dbName)
  }
}
