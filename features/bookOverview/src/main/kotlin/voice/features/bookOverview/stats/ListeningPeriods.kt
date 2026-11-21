package voice.features.bookOverview.stats

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt
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
  val audio: Duration,
  val days: Int,
  val bars: List<Duration>,
  val finished: List<Finish>,
  val hours: List<Duration>,
  val workdayHours: List<Duration>,
  val weekdays: Map<DayOfWeek, Duration>,
  val weeks: Int,
  val longestStretch: Duration,
  val books: Map<BookId, Duration>,
) {
  // the player counts both times apart, so a steady 1.5× can come out as 1.499
  val speed: Float
    get() = if (listened > Duration.ZERO) (audio / listened * 100).roundToInt() / 100F else 1F
}

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
  fun listenedOn(days: Map<LocalDate, List<ListeningSession>>) =
    days.values.sumOf { day -> day.sumOf { it.listenedMillis } }.milliseconds
  val listenedDays = byDay.filterValues { day -> day.any { it.listenedMillis > 0 } }.keys
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
    listened = inPeriod.listened(),
    audio = inPeriod.sumOf { it.audioMillis }.milliseconds,
    days = listenedDays.size,
    bars = bars,
    finished =
      finishes
        .filter { it.day in this }
        .groupBy { it.bookId }
        .map { (_, finishes) -> finishes.maxBy { it.day } }
        .sortedByDescending { it.day },
    hours = inPeriod.hours(),
    workdayHours = inPeriod.filter { it.day().dayOfWeek < DayOfWeek.SATURDAY }.hours(),
    weekdays = inPeriod.groupBy { it.day().dayOfWeek }.mapValues { it.value.listened() },
    weeks =
      listenedDays.distinctBy { it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }.size,
    longestStretch = (inPeriod.maxOfOrNull { it.listenedMillis } ?: 0).milliseconds,
    books = inPeriod.groupBy { it.bookId }.mapValues { it.value.listened() },
  )
}

internal fun List<ListeningSession>.listened(): Duration = sumOf { it.listenedMillis }.milliseconds

internal fun Period.countedDays(
  firstDay: LocalDate,
  today: LocalDate,
): Int =
  ChronoUnit.DAYS.between(maxOf(start, firstDay), minOf(end, today.plusDays(1)))
    .toInt()
    .coerceAtLeast(1)

internal fun Period.habitDays(
  sessions: List<ListeningSession>,
  firstDay: LocalDate,
  today: LocalDate,
): Int {
  // like the streak, today counts only from its first listening
  val open = today in this && sessions.none { it.day() == today && it.listenedMillis > 0 }
  return countedDays(firstDay, today) - if (open) 1 else 0
}

private const val HOUR_MILLIS = 3_600_000L

private fun List<ListeningSession>.hours(): List<Duration> {
  val hours = LongArray(24)
  forEach { session ->
    val start = session.startedAtMillis + session.utcOffsetSeconds * 1000L
    val end = maxOf(start + 1, session.endedAtMillis + session.utcOffsetSeconds * 1000L)
    var at = start
    while (at < end) {
      val next = minOf(end, (at / HOUR_MILLIS + 1) * HOUR_MILLIS)
      hours[(at / HOUR_MILLIS % 24).toInt()] += session.listenedMillis * (next - at) / (end - start)
      at = next
    }
  }
  return hours.map { it.milliseconds }
}

private fun localDate(
  millis: Long,
  utcOffsetSeconds: Int,
): LocalDate =
  Instant.ofEpochMilli(millis).atOffset(ZoneOffset.ofTotalSeconds(utcOffsetSeconds)).toLocalDate()
