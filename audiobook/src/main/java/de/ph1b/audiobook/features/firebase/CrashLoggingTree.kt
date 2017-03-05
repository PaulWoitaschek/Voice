package de.ph1b.audiobook.features.firebase

import com.crashlytics.android.Crashlytics
import timber.log.Timber

/**
 * Timber tree adds logs to crashes
 *
 * @author Paul Woitaschek
 */
class CrashLoggingTree : Timber.Tree() {

  override fun log(priority: Int, tag: String?, message: String?, t: Throwable?) {
    Crashlytics.log(priority, tag, message)
  }
}