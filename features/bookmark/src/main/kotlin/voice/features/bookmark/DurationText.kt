package voice.features.bookmark

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
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
