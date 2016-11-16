package de.ph1b.audiobook.playback.utils

import android.content.SharedPreferences
import android.os.Build
import i
import javax.inject.Inject

/**
 * Provides information about if the custom media player can be used, or if there is a bug on the device.
 *
 * @author Paul Woitaschek
 */
class MediaPlayerCapabilities
@Inject
constructor(private val prefs: SharedPreferences, private val channelDetector: FalseChannelDetector) {

  fun useCustomMediaPlayer(): Boolean {
    // get key
    val contents = listOf<String?>(Build.VERSION.SDK_INT.toString(),
      Build.DEVICE,
      Build.MODEL,
      Build.PRODUCT,
      Build.MANUFACTURER,
      System.getProperty("os.version"),
      Build.BRAND)
    val builder = StringBuilder()
    contents.filter { it != null }
      .forEach { builder.append(it) }
    val key = builder.toString()

    if (prefs.contains(key)) {
      val canSet = prefs.getBoolean(key, false)
      i { "prefs already has the key. CanSet $canSet" }
      return canSet
    } else {
      val canSetCustom = channelDetector.channelCountMatches()
      prefs.edit().putBoolean(key, canSetCustom)
        .apply()
      i { "Channel count matches returned = $canSetCustom" }
      return canSetCustom
    }
  }
}