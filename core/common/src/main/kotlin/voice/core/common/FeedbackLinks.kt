package voice.core.common

import android.os.Build
import androidx.core.net.toUri

/**
 * Where listeners can get help and tell about their ideas and problems.
 */
object FeedbackLinks {

  const val QUESTIONS = "https://github.com/PaulWoitaschek/Voice/discussions/categories/q-a"
  const val IDEAS = "https://github.com/PaulWoitaschek/Voice/discussions/categories/ideas"
  const val FAQ = "https://voice.woitaschek.de/faq/"
  const val EMAIL = "audiobook@posteo.de"

  /**
   * A new bug report, filled in with the app version and the device.
   */
  fun bugReport(versionName: String): String {
    return "https://github.com/PaulWoitaschek/Voice/issues/new".toUri()
      .buildUpon()
      .appendQueryParameter("template", "bug.yml")
      .appendQueryParameter("version", versionName)
      .appendQueryParameter("androidversion", Build.VERSION.SDK_INT.toString())
      .appendQueryParameter("device", Build.MODEL)
      .toString()
  }
}
