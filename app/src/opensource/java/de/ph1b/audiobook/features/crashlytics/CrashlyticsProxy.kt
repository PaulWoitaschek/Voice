package de.ph1b.audiobook.features.crashlytics

import android.app.Application
import de.ph1b.audiobook.misc.ErrorReporter
import de.ph1b.audiobook.persistence.pref.Pref

/**
 * No-Op proxy for crashlytics
 */
@Suppress("UNUSED_PARAMETER")
object CrashlyticsProxy : ErrorReporter {

  override fun log(message: String) {}

  override fun logException(throwable: Throwable) {}

  fun init(app: Application, enabled: Pref<Boolean>) {}
}
