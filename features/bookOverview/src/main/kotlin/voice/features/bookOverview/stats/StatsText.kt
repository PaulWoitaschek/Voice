package voice.features.bookOverview.stats

import android.icu.text.DateIntervalFormat
import android.icu.text.MeasureFormat
import android.icu.util.DateInterval
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.icu.util.TimeZone
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import java.text.FieldPosition
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.Duration
import voice.core.strings.R as StringsR

@Composable
internal fun listenedText(duration: Duration): String {
  val locale = locale()
  val format =
    remember(locale) { MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.NARROW) }
  val hours = duration.inWholeHours
  val minutes = duration.inWholeMinutes % 60
  return when {
    hours > 0 && minutes > 0 ->
      format.formatMeasures(Measure(hours, MeasureUnit.HOUR), Measure(minutes, MeasureUnit.MINUTE))
    hours > 0 -> format.format(Measure(hours, MeasureUnit.HOUR))
    else -> format.format(Measure(minutes, MeasureUnit.MINUTE))
  }
}

@Composable
internal fun PeriodKind.label(): String =
  stringResource(
    when (this) {
      PeriodKind.Week -> StringsR.string.listening_stats_week
      PeriodKind.Month -> StringsR.string.listening_stats_month
      PeriodKind.Year -> StringsR.string.listening_stats_year
      PeriodKind.AllTime -> StringsR.string.listening_stats_all_time
    },
  )

@Composable
internal fun periodText(
  period: Period,
  current: Boolean,
): String {
  if (current) {
    return stringResource(
      when (period.kind) {
        PeriodKind.Week -> StringsR.string.listening_stats_this_week
        PeriodKind.Month -> StringsR.string.listening_stats_this_month
        PeriodKind.Year -> StringsR.string.listening_stats_this_year
        PeriodKind.AllTime -> StringsR.string.listening_stats_all_time
      },
    )
  }
  val locale = locale()
  return remember(period, locale) {
    when (period.kind) {
      PeriodKind.Week -> {
        val last = period.end.minusDays(1)
        val skeleton = if (last.year == LocalDate.now().year) "MMMd" else "yMMMd"
        val format = DateIntervalFormat.getInstance(skeleton, locale)
        format.timeZone = TimeZone.GMT_ZONE
        format
          .format(
            DateInterval(period.start.epochMillis(), last.epochMillis()),
            StringBuffer(),
            FieldPosition(0),
          )
          .toString()
      }
      PeriodKind.Month -> period.start.format(bestPattern(locale, "MMMMyyyy"))
      PeriodKind.Year,
      PeriodKind.AllTime -> period.start.format(bestPattern(locale, "y"))
    }
  }
}

@Composable
internal fun finishedDayText(
  day: LocalDate,
  kind: PeriodKind,
): String {
  val locale = locale()
  return remember(day, kind, locale) {
    val skeleton =
      when (kind) {
        PeriodKind.Week -> "EEEE"
        PeriodKind.Month,
        PeriodKind.Year -> "EEEdMMM"
        PeriodKind.AllTime -> "yMMMd"
      }
    day.format(bestPattern(locale, skeleton))
  }
}

@Composable
internal fun countsSinceText(day: LocalDate): String {
  val locale = locale()
  val date = remember(day, locale) { day.format(bestPattern(locale, "dMMMMyyyy")) }
  return stringResource(StringsR.string.listening_stats_counts_since, date)
}

@Composable
internal fun barLabels(period: Period): List<String> {
  val locale = locale()
  return remember(period, locale) {
    when (period.kind) {
      PeriodKind.Week ->
        List(period.days) {
          period.start.plusDays(it.toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
        }
      PeriodKind.Month ->
        List(period.days) { if (it % 7 == 0) String.format(locale, "%d", it + 1) else "" }
      PeriodKind.Year ->
        List(12) {
          java.time.Month.of(it + 1).getDisplayName(TextStyle.NARROW_STANDALONE, locale)
        }
      PeriodKind.AllTime -> emptyList()
    }
  }
}

@Composable private fun locale(): Locale = LocalConfiguration.current.locales[0]

private fun bestPattern(
  locale: Locale,
  skeleton: String,
): DateTimeFormatter =
  DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)

private fun LocalDate.epochMillis(): Long = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
