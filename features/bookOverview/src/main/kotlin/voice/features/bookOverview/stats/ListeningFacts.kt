package voice.features.bookOverview.stats

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import voice.core.data.Book
import voice.core.data.BookCategory
import voice.core.data.BookId
import voice.core.data.ListeningSession
import voice.core.data.category

@Immutable
internal data class StatsBookViewState(
  val id: BookId,
  val name: String,
  val cover: String?,
  val inLibrary: Boolean,
)

internal fun Book.toStatsBook() = StatsBookViewState(id, name, coverUrl, inLibrary = true)

@Immutable
internal sealed interface WideFact {
  data class WhenYouListen(val hours: List<Duration>) : WideFact {
    val top: DayPart
      get() = DayPart.entries.maxBy { hours.share(it.hours) }

    fun percent(part: DayPart): Int = hours.share(part.hours).percent()
  }

  data class MostListened(
    val book: StatsBookViewState,
    val listened: Duration,
  ) : WideFact

  data class Forecast(
    val book: StatsBookViewState,
    val left: Duration,
    val day: LocalDate,
    val today: LocalDate,
  ) : WideFact
}

@Immutable
internal sealed interface SmallFact {
  data class Change(val by: Duration) : SmallFact

  data class LongestDay(
    val listened: Duration,
    val day: LocalDate,
  ) : SmallFact

  data class LongestMonth(
    val listened: Duration,
    val month: Month,
  ) : SmallFact

  data class Saved(
    val saved: Duration,
    val speed: Float,
  ) : SmallFact

  data class LongestStretch(val listened: Duration) : SmallFact

  data class YourDay(val day: DayOfWeek) : SmallFact

  data class Streak(
    val current: Int,
    val longest: Int,
  ) : SmallFact
}

internal fun <T> List<T>.pick(
  day: LocalDate,
  count: Int,
): List<T> {
  if (size <= count) return this
  return List(count) { Math.floorMod(day.toEpochDay() * count + it, size) }
    .sorted()
    .map { this[it] }
}

internal fun PeriodStats.whenYouListen(): WideFact.WhenYouListen? =
  WideFact.WhenYouListen(hours).takeIf { listened >= 2.hours }

internal fun PeriodStats.mostListened(
  book: (BookId) -> StatsBookViewState?,
): WideFact.MostListened? {
  val books = books.filterValues { it >= 1.minutes }
  if (books.size < 2) return null
  val (id, listened) = books.maxBy { it.value }
  return book(id)?.let { WideFact.MostListened(it, listened) }
}

internal fun forecast(
  sessions: List<ListeningSession>,
  books: List<Book>,
  firstDay: LocalDate,
  today: LocalDate,
): WideFact.Forecast? {
  val days = (ChronoUnit.DAYS.between(firstDay, today) + 1).coerceIn(1, 14)
  val from = today.minusDays(days - 1)
  val recent = sessions.filter { it.day() in from..today && it.listenedMillis > 0 }
  val inProgress =
    books.filter { it.category == BookCategory.CURRENT && !it.custom.hidden }.associateBy { it.id }
  val book =
    recent.sortedByDescending { it.endedAtMillis }.firstNotNullOfOrNull { inProgress[it.bookId] }
      ?: return null
  val speed = book.content.playbackSpeed.coerceAtLeast(0.1F)
  val left = ((book.duration - book.position) / speed).toLong().milliseconds
  val pace = recent.listened() / days.toInt()
  if (left < 1.minutes) return null
  val daysAway = ceil(left / pace).toLong() - 1
  if (daysAway > 60) return null
  return WideFact.Forecast(book.toStatsBook(), left, today.plusDays(daysAway), today)
}

internal fun Period.change(
  listened: Duration,
  before: Period,
  sessions: List<ListeningSession>,
  firstDay: LocalDate,
  today: LocalDate,
): SmallFact.Change? {
  if (kind == PeriodKind.AllTime || firstDay > before.start) return null
  val beforeEnd =
    if (today in this) {
      minOf(before.end, before.start.plusDays(ChronoUnit.DAYS.between(start, today) + 1))
    } else {
      before.end
    }
  val by =
    listened - sessions.filter { it.day() >= before.start && it.day() < beforeEnd }.listened()
  return SmallFact.Change(by).takeIf { by.absoluteValue >= 1.minutes }
}

internal fun Period.longest(bars: List<Duration>): SmallFact? {
  if (bars.count { it > Duration.ZERO } < 2) return null
  val index = bars.indices.maxBy { bars[it] }
  if (bars[index] < 1.minutes) return null
  return if (kind == PeriodKind.Year) {
    SmallFact.LongestMonth(bars[index], Month.of(index + 1))
  } else {
    SmallFact.LongestDay(bars[index], start.plusDays(index.toLong()))
  }
}

internal fun PeriodStats.saved(): SmallFact.Saved? =
  SmallFact.Saved(audio - listened, speed).takeIf { speed >= 1.25F && it.saved >= 1.minutes }

internal fun PeriodStats.longestStretch(): SmallFact.LongestStretch? =
  SmallFact.LongestStretch(longestStretch).takeIf { longestStretch >= 1.hours }

internal fun PeriodStats.yourDay(kind: PeriodKind): SmallFact.YourDay? {
  if (kind == PeriodKind.Week || weeks < 4) return null
  return SmallFact.YourDay(weekdays.maxBy { it.value }.key)
}

internal fun streak(
  sessions: List<ListeningSession>,
  today: LocalDate,
): SmallFact.Streak? {
  val runs = mutableMapOf<LocalDate, Int>()
  sessions
    .filter { it.listenedMillis > 0 }
    .map { it.day() }
    .toSortedSet()
    .forEach { runs[it] = (runs[it.minusDays(1)] ?: 0) + 1 }
  val longest = runs.values.maxOrNull() ?: 0
  // today counts only from its first listening, so the run of yesterday stays until midnight
  val current = runs[today] ?: runs[today.minusDays(1)] ?: 0
  return SmallFact.Streak(current, longest).takeIf { longest >= 2 }
}
