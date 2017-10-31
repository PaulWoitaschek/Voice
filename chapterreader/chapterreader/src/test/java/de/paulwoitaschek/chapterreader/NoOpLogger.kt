package de.paulwoitaschek.chapterreader

import de.paulwoitaschek.chapterreader.misc.Logger

object NoOpLogger : Logger {
  override fun e(message: String, cause: Throwable?) {}
  override fun i(message: String) {}
  override fun e(message: String) {}
  override fun e(throwable: Throwable) {}
}
