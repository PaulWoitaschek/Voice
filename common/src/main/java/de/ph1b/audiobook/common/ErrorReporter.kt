package de.ph1b.audiobook.common


interface ErrorReporter {

  fun log(priority: Int, tag: String?, message: String?)

  fun logException(throwable: Throwable)
}
