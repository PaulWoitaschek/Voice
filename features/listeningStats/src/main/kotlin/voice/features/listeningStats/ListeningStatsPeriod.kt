package voice.features.listeningStats

import androidx.annotation.StringRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import voice.core.strings.R as StringsR

// Stats periods are limited to the current day, week, and month for a lightweight per-book review.
internal enum class ListeningStatsPeriod(@StringRes val titleRes: Int) {
  // The day view answers how long the book was played today, matching the long-press entry.
  Day(StringsR.string.listening_stats_period_day),

  // The week view follows the system locale's week start instead of imposing one calendar convention.
  Week(StringsR.string.listening_stats_period_week),

  // The month view uses natural months so users read it as a by-product of reading progress.
  Month(StringsR.string.listening_stats_period_month),
  ;

  // The UI only needs the current period bounds; paging through history is left to a future full review page.
  fun range(
    now: Instant,
    zoneId: ZoneId,
  ): ListeningStatsRange {
    val today = now.atZone(zoneId).toLocalDate()
    val startDate = when (this) {
      Day -> today
      Week -> today.startOfWeek()
      Month -> today.withDayOfMonth(1)
    }
    val endDate = when (this) {
      Day -> startDate.plusDays(1)
      Week -> startDate.plusWeeks(1)
      Month -> startDate.plusMonths(1)
    }
    return ListeningStatsRange(
      startedAt = startDate.atStartOfDay(zoneId).toInstant(),
      endedAt = endDate.atStartOfDay(zoneId).toInstant(),
    )
  }

  // Week start comes from the system locale so international users are not confused by Sunday vs Monday.
  private fun LocalDate.startOfWeek(): LocalDate {
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val daysSinceStart = (dayOfWeek.value - firstDayOfWeek.value + DAYS_IN_WEEK) % DAYS_IN_WEEK
    return minusDays(daysSinceStart.toLong())
  }

  private companion object {
    // ISO day-of-week uses 1..7; a named constant avoids a magic number.
    const val DAYS_IN_WEEK = 7
  }
}

// Range objects hand Instant bounds to the data layer so the DAO never learns local calendar rules.
internal data class ListeningStatsRange(
  val startedAt: Instant,
  val endedAt: Instant,
)
