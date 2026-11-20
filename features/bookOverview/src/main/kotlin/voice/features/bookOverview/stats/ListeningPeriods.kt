package voice.features.bookOverview.stats

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaInstant
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.ListeningSession

enum class PeriodKind(internal val analyticsName: String) {
  Week("week"),
  Month("month"),
  Year("year"),
  AllTime("all_time"),
}

internal data class Period(
  val kind: PeriodKind,
  val start: LocalDate,
  val end: LocalDate,
) {
  operator fun contains(day: LocalDate): Boolean = day >= start && day < end

  val days: Int
    get() = ChronoUnit.DAYS.between(start, end).toInt()
}

internal fun PeriodKind.period(
  today: LocalDate,
  back: Int,
  weekStart: DayOfWeek,
): Period =
  when (this) {
    PeriodKind.Week -> {
      val start = today.with(TemporalAdjusters.previousOrSame(weekStart)).minusWeeks(back.toLong())
      Period(this, start, start.plusWeeks(1))
    }
    PeriodKind.Month -> {
      val start = today.withDayOfMonth(1).minusMonths(back.toLong())
      Period(this, start, start.plusMonths(1))
    }
    PeriodKind.Year -> {
      val start = today.withDayOfYear(1).minusYears(back.toLong())
      Period(this, start, start.plusYears(1))
    }
    PeriodKind.AllTime -> Period(this, LocalDate.MIN, LocalDate.MAX)
  }

internal data class Finish(
  val bookId: BookId,
  val day: LocalDate,
)

internal data class PeriodStats(
  val listened: Duration,
  val saved: Duration,
  val days: Int,
  val bars: List<Duration>,
  val finished: List<Finish>,
)

internal fun ListeningSession.day(): LocalDate = localDate(startedAtMillis, utcOffsetSeconds)

internal fun finishes(
  sessions: List<ListeningSession>,
  contents: List<BookContent>,
  zone: ZoneId,
): List<Finish> {
  val played =
    sessions
      .filter { it.reachedEnd }
      .map { Finish(it.bookId, localDate(it.endedAtMillis, it.utcOffsetSeconds)) }
  // a renamed folder leaves the old content inactive with a copy of the mark
  val marked =
    contents
      .filter { it.isActive }
      .mapNotNull { content ->
        val at = content.markedCompletedAt?.takeIf { content.startedBeforeMark }
        at?.let { Finish(content.id, it.toJavaInstant().atZone(zone).toLocalDate()) }
      }
  return played + marked
}

internal fun Period.stats(
  sessions: List<ListeningSession>,
  finishes: List<Finish>,
): PeriodStats {
  val byDay = sessions.groupBy { it.day() }.filterKeys { it in this }
  val inPeriod = byDay.values.flatten()
  val listened = inPeriod.sumOf { it.listenedMillis }.milliseconds
  val audio = inPeriod.sumOf { it.audioMillis }.milliseconds
  fun listenedOn(days: Map<LocalDate, List<ListeningSession>>) =
    days.values.sumOf { day -> day.sumOf { it.listenedMillis } }.milliseconds
  val bars =
    when (kind) {
      PeriodKind.Week,
      PeriodKind.Month ->
        List(days) { listenedOn(byDay.filterKeys { day -> day == start.plusDays(it.toLong()) }) }
      PeriodKind.Year ->
        List(12) { listenedOn(byDay.filterKeys { day -> day.monthValue == it + 1 }) }
      PeriodKind.AllTime -> emptyList()
    }
  return PeriodStats(
    listened = listened,
    saved = (audio - listened).coerceAtLeast(Duration.ZERO),
    days = byDay.count { (_, sessions) -> sessions.any { it.listenedMillis > 0 } },
    bars = bars,
    finished =
      finishes
        .filter { it.day in this }
        .groupBy { it.bookId }
        .map { (_, finishes) -> finishes.maxBy { it.day } }
        .sortedByDescending { it.day },
  )
}

private fun localDate(
  millis: Long,
  utcOffsetSeconds: Int,
): LocalDate =
  Instant.ofEpochMilli(millis).atOffset(ZoneOffset.ofTotalSeconds(utcOffsetSeconds)).toLocalDate()
