package voice.app.scanner

import voice.logging.core.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Parses a vorbis chapter duration, i.e. 01:54:12.8
 * See [Chapter Extension](https://wiki.xiph.org/Chapter_Extension)
 */
internal fun parseVorbisDuration(value: String): Duration? {
  val split = value.split(":")
  return if (split.size == 3) {
    val hour = split[0].toLongOrNull()?.hours
    val minute = split[1].toLongOrNull()?.minutes
    val seconds = split[2].toDoubleOrNull()?.seconds
    if (hour != null && minute != null && seconds != null) {
      hour + minute + seconds
    } else {
      Logger.w("Invalid vorbis chapter format: $value")
      null
    }
  } else {
    Logger.w("Invalid vorbis chapter format: $value")
    null
  }
}
