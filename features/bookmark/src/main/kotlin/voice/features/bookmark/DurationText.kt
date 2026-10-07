package voice.features.bookmark

import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.icu.text.MeasureFormat
import android.icu.text.NumberFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.icu.util.ULocale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale
import kotlin.time.Duration

/**
 * A duration as people say it, like "1 hr, 29 min" or "45 sec", in the user's language.
 */
@Composable
internal fun durationText(duration: Duration): String {
  val locale = LocalConfiguration.current.locales[0]
  val format = remember(locale) { MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT) }
  return duration.toComponents { hours, minutes, seconds, _ ->
    when {
      hours > 0 && minutes > 0 -> format.formatMeasures(Measure(hours, MeasureUnit.HOUR), Measure(minutes, MeasureUnit.MINUTE))
      hours > 0 -> format.format(Measure(hours, MeasureUnit.HOUR))
      minutes > 0 -> format.format(Measure(minutes, MeasureUnit.MINUTE))
      else -> format.format(Measure(seconds, MeasureUnit.SECOND))
    }
  }
}

/**
 * Seconds with their sign, like "+15s" or "-15s", in the user's language.
 */
@Composable
internal fun signedSecondsText(seconds: Long): String {
  val locale = LocalConfiguration.current.locales[0]
  val format = remember(locale) { signedSecondsFormat(locale) }
  return format.format(Measure(seconds, MeasureUnit.SECOND))
}

internal fun signedSecondsFormat(locale: Locale): MeasureFormat {
  val number = NumberFormat.getIntegerInstance(locale)
  if (number is DecimalFormat) {
    val symbols = DecimalFormatSymbols.getInstance(locale)
    number.positivePrefix = symbols.plusSignString
    number.negativePrefix = symbols.minusSignString
  }
  return MeasureFormat.getInstance(ULocale.forLocale(locale), MeasureFormat.FormatWidth.NARROW, number)
}
