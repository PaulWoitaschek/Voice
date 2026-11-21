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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.FieldPosition
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
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
): String =
  dayText(
    day,
    skeleton =
      when (kind) {
        PeriodKind.Week -> "EEEE"
        PeriodKind.Month,
        PeriodKind.Year -> "EEEdMMM"
        PeriodKind.AllTime -> "yMMMd"
      },
  )

@Composable
private fun dayText(
  day: LocalDate,
  skeleton: String,
): String {
  val locale = locale()
  return remember(day, skeleton, locale) { day.format(bestPattern(locale, skeleton)) }
}

@Composable
internal fun percentText(percent: Int): String {
  val locale = locale()
  return remember(percent, locale) {
    NumberFormat.getPercentInstance(locale).format(percent / 100.0)
  }
}

@Composable
internal fun countText(count: Int): String {
  val locale = locale()
  return remember(count, locale) { NumberFormat.getIntegerInstance(locale).format(count) }
}

@Composable
private fun speedText(speed: Float): String {
  val locale = locale()
  return remember(speed, locale) {
    DecimalFormat("0.0#", DecimalFormatSymbols.getInstance(locale)).format(speed) + "×"
  }
}

@Composable
internal fun hourLabels(hours: List<Int>): List<String> {
  val locale = locale()
  val step = if (LocalDensity.current.fontScale >= 1.5F) 12 else 6
  return remember(hours, locale, step) {
    val format = bestPattern(locale, "j")
    hours.map { if (it % step == 0) LocalTime.of(it, 0).format(format) else "" }
  }
}

@Composable
internal fun DayPart.label(): String =
  stringResource(
    when (this) {
      DayPart.Morning -> StringsR.string.listening_stats_part_morning
      DayPart.Afternoon -> StringsR.string.listening_stats_part_afternoon
      DayPart.Evening -> StringsR.string.listening_stats_part_evening
      DayPart.Night -> StringsR.string.listening_stats_part_night
    },
  )

@Composable
internal fun ListenerName.text(): String =
  stringResource(
    when (this) {
      is ListenerName.Audiomaniac -> StringsR.string.listening_stats_name_audiomaniac
      is ListenerName.MarathonListener -> StringsR.string.listening_stats_name_marathon
      is ListenerName.NightOwl -> StringsR.string.listening_stats_name_night_owl
      is ListenerName.EarlyBird -> StringsR.string.listening_stats_name_early_bird
      ListenerName.Commuter -> StringsR.string.listening_stats_name_commuter
      is ListenerName.Juggler -> StringsR.string.listening_stats_name_juggler
      is ListenerName.Speedster -> StringsR.string.listening_stats_name_speedster
      is ListenerName.WeekendListener -> StringsR.string.listening_stats_name_weekend
      ListenerName.CreatureOfHabit -> StringsR.string.listening_stats_name_habit
      is ListenerName.PartOfDay ->
        when (part) {
          DayPart.Afternoon -> StringsR.string.listening_stats_name_afternoon
          DayPart.Evening -> StringsR.string.listening_stats_name_evening
          DayPart.Morning,
          DayPart.Night -> StringsR.string.listening_stats_name_morning
        }
    },
  )

@Composable
internal fun ListenerName.reason(): String =
  when (this) {
    is ListenerName.Audiomaniac ->
      stringResource(StringsR.string.listening_stats_name_audiomaniac_reason, listenedText(perDay))
    is ListenerName.MarathonListener ->
      stringResource(StringsR.string.listening_stats_name_marathon_reason, listenedText(stretch))
    is ListenerName.NightOwl ->
      stringResource(StringsR.string.listening_stats_name_night_owl_reason, percentText(percent))
    is ListenerName.EarlyBird ->
      stringResource(StringsR.string.listening_stats_name_early_bird_reason, percentText(percent))
    ListenerName.Commuter -> stringResource(StringsR.string.listening_stats_name_commuter_reason)
    is ListenerName.Juggler ->
      pluralStringResource(StringsR.plurals.listening_stats_name_juggler_reason, books, books)
    is ListenerName.Speedster ->
      stringResource(StringsR.string.listening_stats_name_speedster_reason, speedText(speed))
    is ListenerName.WeekendListener ->
      stringResource(StringsR.string.listening_stats_name_weekend_reason, percentText(percent))
    ListenerName.CreatureOfHabit ->
      stringResource(StringsR.string.listening_stats_name_habit_reason)
    is ListenerName.PartOfDay ->
      stringResource(
        when (part) {
          DayPart.Afternoon -> StringsR.string.listening_stats_name_afternoon_reason
          DayPart.Evening -> StringsR.string.listening_stats_name_evening_reason
          DayPart.Morning,
          DayPart.Night -> StringsR.string.listening_stats_name_morning_reason
        },
        percentText(percent),
      )
  }

@Composable
internal fun WideFact.Forecast.dayText(): String =
  when (ChronoUnit.DAYS.between(today, day)) {
    0L -> stringResource(StringsR.string.listening_stats_forecast_today)
    1L -> stringResource(StringsR.string.listening_stats_forecast_tomorrow)
    in 2L..6L ->
      stringResource(StringsR.string.listening_stats_forecast_weekday, dayText(day, "EEEE"))
    else -> stringResource(StringsR.string.listening_stats_forecast_date, dayText(day, "dMMM"))
  }

@Composable
internal fun SmallFact.value(): String =
  when (this) {
    is SmallFact.Change -> (if (by > Duration.ZERO) "+" else "−") + listenedText(by.absoluteValue)
    is SmallFact.LongestDay -> listenedText(listened)
    is SmallFact.LongestMonth -> listenedText(listened)
    is SmallFact.Saved -> listenedText(saved)
    is SmallFact.LongestStretch -> listenedText(listened)
    is SmallFact.YourDay -> {
      val locale = locale()
      day.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar {
        it.titlecase(locale)
      }
    }
    is SmallFact.Streak -> {
      val days = if (current >= 2) current else longest
      pluralStringResource(StringsR.plurals.listening_stats_streak_days, days, days)
    }
  }

@Composable
internal fun SmallFact.label(
  kind: PeriodKind,
  currentPeriod: Boolean,
): String =
  when (this) {
    is SmallFact.Change -> {
      val more = by > Duration.ZERO
      stringResource(
        when (kind) {
          PeriodKind.Month ->
            when {
              more && currentPeriod -> StringsR.string.listening_stats_change_more_month
              more -> StringsR.string.listening_stats_change_more_month_before
              currentPeriod -> StringsR.string.listening_stats_change_less_month
              else -> StringsR.string.listening_stats_change_less_month_before
            }
          PeriodKind.Year ->
            when {
              more && currentPeriod -> StringsR.string.listening_stats_change_more_year
              more -> StringsR.string.listening_stats_change_more_year_before
              currentPeriod -> StringsR.string.listening_stats_change_less_year
              else -> StringsR.string.listening_stats_change_less_year_before
            }
          PeriodKind.Week,
          PeriodKind.AllTime ->
            when {
              more && currentPeriod -> StringsR.string.listening_stats_change_more_week
              more -> StringsR.string.listening_stats_change_more_week_before
              currentPeriod -> StringsR.string.listening_stats_change_less_week
              else -> StringsR.string.listening_stats_change_less_week_before
            }
        },
      )
    }
    is SmallFact.LongestDay ->
      if (kind == PeriodKind.Week) {
        stringResource(StringsR.string.listening_stats_longest_weekday, dayText(day, "EEEE"))
      } else {
        stringResource(StringsR.string.listening_stats_longest_date, dayText(day, "EEEdMMM"))
      }
    is SmallFact.LongestMonth ->
      stringResource(
        StringsR.string.listening_stats_longest_month,
        month.getDisplayName(TextStyle.FULL_STANDALONE, locale()),
      )
    is SmallFact.Saved -> stringResource(StringsR.string.listening_stats_saved_at, speedText(speed))
    is SmallFact.LongestStretch -> stringResource(StringsR.string.listening_stats_stretch)
    is SmallFact.YourDay -> stringResource(StringsR.string.listening_stats_your_day)
    is SmallFact.Streak ->
      when {
        current < 2 -> stringResource(StringsR.string.listening_stats_streak_longest)
        current >= longest -> stringResource(StringsR.string.listening_stats_streak_row_longest)
        else -> pluralStringResource(StringsR.plurals.listening_stats_streak_row, longest, longest)
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
