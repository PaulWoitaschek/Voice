package de.ph1b.audiobook.misc

interface ErrorReporter {

  fun log(message: String)
  fun logException(throwable: Throwable)
}
