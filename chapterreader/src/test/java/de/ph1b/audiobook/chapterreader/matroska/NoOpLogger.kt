package de.ph1b.audiobook.chapterreader.matroska

import de.ph1b.audiobook.common.Logger


object NoOpLogger : Logger {
  override fun i(message: String) {}
  override fun e(message: String) {}
  override fun e(throwable: Throwable) {}
}
