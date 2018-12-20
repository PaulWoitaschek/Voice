package de.ph1b.audiobook.crashreporting

import android.app.Application

/**
 * No-Op crash reporter
 */
@Suppress("UNUSED_PARAMETER")
object CrashReporter {

  fun log(message: String) {}

  fun logException(throwable: Throwable) {}

  fun init(app: Application) {}
}
