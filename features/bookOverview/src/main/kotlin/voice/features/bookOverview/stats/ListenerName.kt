package voice.features.bookOverview.stats

import java.time.DayOfWeek
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal enum class DayPart(val hours: List<Int>) {
  Morning((5..11).toList()),
  Afternoon((12..16).toList()),
  Evening((17..21).toList()),
  Night(listOf(22, 23, 0, 1, 2, 3, 4)),
}

internal sealed interface ListenerName {
  data class Audiomaniac(val perDay: Duration) : ListenerName

  data class MarathonListener(val stretch: Duration) : ListenerName

  data class NightOwl(val percent: Int) : ListenerName

  data class EarlyBird(val percent: Int) : ListenerName

  data object Commuter : ListenerName

  data class Juggler(val books: Int) : ListenerName

  data class Speedster(val speed: Float) : ListenerName

  data class WeekendListener(val percent: Int) : ListenerName

  data object CreatureOfHabit : ListenerName

  data class PartOfDay(
    val part: DayPart,
    val percent: Int,
  ) : ListenerName
}

internal fun List<Duration>.share(hours: Iterable<Int>): Double {
  val all = fold(Duration.ZERO, Duration::plus)
  if (all == Duration.ZERO) return 0.0
  return hours.fold(Duration.ZERO) { sum, hour -> sum + this[hour] } / all
}

internal fun Double.percent(): Int = (this * 100).roundToInt()

internal fun PeriodStats.names(
  kind: PeriodKind,
  countedDays: Int,
  habitDays: Int,
): List<ListenerName> {
  if (listened < 2.hours) return emptyList()
  val names = buildList {
    val perDay = listened / countedDays
    if (perDay >= 2.hours) add(ListenerName.Audiomaniac(perDay))
    if (longestStretch >= 3.hours) add(ListenerName.MarathonListener(longestStretch))
    val night = hours.share(DayPart.Night.hours)
    if (night >= 0.4) add(ListenerName.NightOwl(night.percent()))
    val early = hours.share(5..8)
    if (early >= 0.4) add(ListenerName.EarlyBird(early.percent()))
    fun commute(hours: IntRange) =
      hours.fold(Duration.ZERO) { sum, hour -> sum + workdayHours[hour] } / listened
    if (commute(6..8) >= 0.25 && commute(16..18) >= 0.25) add(ListenerName.Commuter)
    val juggled = books.count { it.value >= 30.minutes }
    // over a month or a year, the books come one after the other
    if (juggled >= 3 && kind == PeriodKind.Week) add(ListenerName.Juggler(juggled))
    if (speed >= 1.5F) add(ListenerName.Speedster(speed))
    val saturday = weekdays[DayOfWeek.SATURDAY] ?: Duration.ZERO
    val sunday = weekdays[DayOfWeek.SUNDAY] ?: Duration.ZERO
    val weekend = (saturday + sunday) / listened
    if (weekend >= 0.6 && countedDays >= 7) add(ListenerName.WeekendListener(weekend.percent()))
    if (habitDays >= 5 && days >= habitDays) add(ListenerName.CreatureOfHabit)
  }
  if (names.isNotEmpty()) return names
  val part =
    listOf(DayPart.Morning, DayPart.Afternoon, DayPart.Evening).maxBy { hours.share(it.hours) }
  return listOf(ListenerName.PartOfDay(part, hours.share(part.hours).percent()))
}
