package de.ph1b.audiobook

import android.app.Application
import timber.log.Timber

/**
 * Application sub class for usage in testing.
 */
class TestApp : Application() {

  override fun onCreate() {
    super.onCreate()

    if (Timber.treeCount() == 0) Timber.plant(Timber.DebugTree())
  }
}
