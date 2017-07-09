package de.ph1b.audiobook.features.crashlytics

import android.app.Application

/**
 * No-Op proxy for crashlytics
 */
@Suppress("UNUSED_PARAMETER")
object CrashlyticsProxy {

  fun log(priority: Int, tag: String?, message: String?) {}

  fun logException(throwable: Throwable) {}

  fun init(app: Application) {}
}
