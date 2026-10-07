package voice.features.bookmark

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import voice.core.strings.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

@Composable
internal fun dayLabelText(label: DayLabel): String {
  val locale = LocalConfiguration.current.locales[0]
  return when (label) {
    DayLabel.JustNow -> stringResource(R.string.bookmark_created_just_now)
    is DayLabel.Today -> timeText(label.time)
    DayLabel.ThisMorning -> stringResource(R.string.time_relative_this_morning)
    DayLabel.ThisAfternoon -> stringResource(R.string.time_relative_this_afternoon)
    DayLabel.ThisEvening -> stringResource(R.string.time_relative_this_evening)
    DayLabel.LastNight -> stringResource(R.string.time_relative_last_night)
    DayLabel.Yesterday -> stringResource(R.string.time_relative_yesterday)
    is DayLabel.Weekday -> label.day.getDisplayName(TextStyle.SHORT, locale)
    is DayLabel.Date -> {
      val pattern = if (label.withYear) "d MMM yyyy" else "d MMM"
      val formatter = remember(locale, pattern) {
        DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, pattern), locale)
      }
      formatter.format(label.date)
    }
  }
}

/**
 * A time of day, with or without AM and PM as the system's 24-hour setting says.
 */
@Composable
internal fun timeText(time: LocalTime): String {
  val context = LocalContext.current
  val locale = LocalConfiguration.current.locales[0]
  val format = remember(context, locale) {
    TimeOfDayFormat(locale, is24HourFormat = android.text.format.DateFormat.is24HourFormat(context))
  }
  return format.format(time)
}

/**
 * Formats with ICU itself: some languages' 12-hour patterns use a day period like "in the
 * afternoon", which java.time can't read before Android 14.
 */
internal class TimeOfDayFormat(
  locale: Locale,
  is24HourFormat: Boolean,
) {

  private val format = DateFormat.getInstanceForSkeleton(if (is24HourFormat) "Hm" else "hm", locale).apply {
    timeZone = TimeZone.GMT_ZONE
  }

  fun format(time: LocalTime): String = format.format(Date(time.toSecondOfDay() * 1000L))
}
