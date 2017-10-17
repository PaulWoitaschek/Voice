package de.ph1b.audiobook.common

interface ErrorReporter {

  fun log(message: String)

  fun logException(throwable: Throwable)
}
