package de.ph1b.audiobook.misc

import de.ph1b.audiobook.common.Logger
import timber.log.Timber


object TimberLogger : Logger {

  override fun i(message: String) {
    Timber.i(message)
  }

  override fun e(message: String) {
    Timber.e(message)
  }

  override fun e(throwable: Throwable) {
    Timber.e(throwable)
  }
}
