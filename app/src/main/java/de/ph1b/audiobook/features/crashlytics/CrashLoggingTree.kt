package de.ph1b.audiobook.features.crashlytics

import timber.log.Timber

/**
 * Timber tree adds logs to crashes
 */
class CrashLoggingTree : Timber.Tree() {

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    CrashlyticsProxy.log(message)
  }
}
