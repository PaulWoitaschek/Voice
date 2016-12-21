package de.ph1b.audiobook.features.firebase

import android.util.Log

import timber.log.Timber

/**
 * Timber tree adds logs to crashes
 *
 * @author Paul Woitaschek
 */
class FirebaseCrashReportingTree : Timber.Tree() {

  override fun log(priority: Int, tag: String?, message: String?, t: Throwable?) {
    if (priority >= Log.DEBUG && message != null) {
      FirebaseCrashProxy.log(message)
    }
  }
}