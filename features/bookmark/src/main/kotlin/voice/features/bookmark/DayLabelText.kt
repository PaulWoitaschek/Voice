package voice.features.bookmark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import voice.core.strings.R
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

@Composable
internal fun dayLabelText(label: DayLabel): String {
  val locale = LocalConfiguration.current.locales[0]
  return when (label) {
    DayLabel.JustNow -> stringResource(R.string.bookmark_created_just_now)
    is DayLabel.Today -> {
      val formatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
      formatter.format(label.time)
    }
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

@Composable
internal fun timeText(time: java.time.LocalTime): String {
  val locale = LocalConfiguration.current.locales[0]
  val formatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
  return formatter.format(time)
}
