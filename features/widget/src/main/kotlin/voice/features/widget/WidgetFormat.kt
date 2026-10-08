package voice.features.widget

import android.content.Context
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.text.format.DateFormat
import java.text.DecimalFormat
import java.time.Instant
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import voice.core.strings.R as StringsR

internal fun Context.formatDuration(
  duration: Duration,
  compact: Boolean = false,
): String {
  val format = MeasureFormat.getInstance(resources.configuration.locales[0], MeasureFormat.FormatWidth.SHORT)
  val rounded = if (compact && duration >= 1.hours) {
    (duration + 30.minutes).inWholeHours.hours
  } else {
    duration
  }
  return rounded.toComponents { hours, minutes, _, _ ->
    when {
      hours > 0 && minutes > 0 -> format.formatMeasures(Measure(hours, MeasureUnit.HOUR), Measure(minutes, MeasureUnit.MINUTE))
      hours > 0 -> format.format(Measure(hours, MeasureUnit.HOUR))
      else -> format.format(Measure(minutes.coerceAtLeast(1), MeasureUnit.MINUTE))
    }
  }
}

internal fun Context.formatTimeLeft(
  remaining: Duration,
  compact: Boolean = false,
): String {
  return getString(StringsR.string.playback_book_progress_remaining, formatDuration(remaining, compact))
}

internal fun Context.formatClockTime(instant: Instant): String {
  return DateFormat.getTimeFormat(this).format(Date.from(instant))
}

internal fun formatSpeed(speed: Float): String = DecimalFormat("0.0#").format(speed) + "×"
