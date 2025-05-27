package voice.settings.views.sleeptimer

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun rememberLocalTimeFormatter(): DateTimeFormatter {
  val context = LocalContext.current
  val locale = LocalConfiguration.current.locales.get(0)!!
  return remember(context, locale) {
    val is24HourFormat = DateFormat.is24HourFormat(context)
    localTimeFormatter(is24HourFormat = is24HourFormat, locale = locale)
  }
}

internal fun localTimeFormatter(
  is24HourFormat: Boolean,
  locale: Locale,
): DateTimeFormatter {
  val pattern = if (is24HourFormat) "HH:mm" else "hh:mm a"
  return DateTimeFormatter.ofPattern(pattern, locale)
}
