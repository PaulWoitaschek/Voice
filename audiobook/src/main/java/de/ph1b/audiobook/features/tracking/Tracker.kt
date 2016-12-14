package de.ph1b.audiobook.features.tracking

import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.firebase.analytics.FirebaseAnalytics
import de.ph1b.audiobook.features.book_overview.BookShelfController
import de.ph1b.audiobook.uitools.ThemeUtil
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracker for firebase analytics
 *
 * @author Paul Woitaschek
 */
@Singleton class Tracker @Inject constructor(private val analytics: FirebaseAnalytics) {

  fun track(controller: Controller) {
    analytics.setCurrentScreen(controller.activity!!, controller.javaClass.name, null)
  }

  fun displayModeChanged(displayMode: BookShelfController.DisplayMode) = logSimple("displayMode") { putString(it, displayMode.name) }

  fun track(theme: ThemeUtil.Theme) = logSimple("theme") { putString(it, theme.name) }

  fun resumePlaybackOnHeadset(resume: Boolean) = logSimple("resumePlaybackOnHeadset") { putBoolean(it, resume) }

  fun pauseOnInterruption(pauseOnInterruption: Boolean) = logSimple("pauseOnInterruption") { putBoolean(it, pauseOnInterruption) }

  fun autoRewindAmount(amount: Int) = logSimple("autoRewindAmount") { putInt(it, amount) }

  fun seekTime(seekTime: Int) = logSimple("seekTime") { putInt(it, seekTime) }

  fun addedFolder(isCollectionFolder: Boolean) = logSimple("folderAdded") { putBoolean("isCollectionFolder", isCollectionFolder) }

  /** extension function that logs an event and executes the supplied function on the bundle before sending it */
  private inline fun logSimple(eventName: String, bundleFunction: Bundle.(event: String) -> Unit) {
    analytics.logEvent(eventName, Bundle().apply {
      bundleFunction(eventName)
    })
  }
}