package de.ph1b.audiobook.common

/**
 * todo
 */
interface ErrorReporter {

  fun log(priority: Int, tag: String?, message: String?)

  fun logException(throwable: Throwable)
}
