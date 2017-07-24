package de.ph1b.audiobook.features.crashlytics

import android.app.Application
import de.ph1b.audiobook.common.ErrorReporter

/**
 * No-Op proxy for crashlytics
 */
@Suppress("UNUSED_PARAMETER")
object CrashlyticsProxy : ErrorReporter {

  override fun log(priority: Int, tag: String?, message: String?) {}

  override fun logException(throwable: Throwable) {}

  fun init(app: Application) {}
}
