package voice.features.bookmark

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/** When something happened, in the words people use: "Just now", "Last night", "Thu", "28 Sep". */
internal sealed interface DayLabel {
  data object JustNow : DayLabel

  data class Today(val time: LocalTime) : DayLabel

  data object ThisMorning : DayLabel

  data object ThisAfternoon : DayLabel

  data object ThisEvening : DayLabel

  data object LastNight : DayLabel

  data object Yesterday : DayLabel

  data class Weekday(val day: DayOfWeek) : DayLabel

  data class Date(
    val date: LocalDate,
    val withYear: Boolean,
  ) : DayLabel
}

// a night lasts until 5 in the morning, so a sleep timer ending at 1:00 belongs to last night
private val nightEnd = LocalTime.of(5, 0)
private val eveningStart = LocalTime.of(18, 0)
private val afternoonStart = LocalTime.of(12, 0)

internal fun savedAtLabel(
  at: Instant,
  now: Instant,
  zone: ZoneId,
): DayLabel {
  if (now - at < 1.minutes) {
    return DayLabel.JustNow
  }
  val dateTime = at.toJavaInstant().atZone(zone)
  val today = now.toJavaInstant().atZone(zone).toLocalDate()
  if (dateTime.toLocalDate() == today) {
    return DayLabel.Today(dateTime.toLocalTime().withSecond(0).withNano(0))
  }
  return dayLabel(dateTime.toLocalDate(), today)
}

internal fun sessionLabel(
  at: Instant,
  now: Instant,
  zone: ZoneId,
): DayLabel {
  val dateTime = at.toJavaInstant().atZone(zone)
  val date = dateTime.toLocalDate()
  val time = dateTime.toLocalTime()
  val today = now.toJavaInstant().atZone(zone).toLocalDate()
  return when {
    isLastNight(date, time, today) -> DayLabel.LastNight
    date == today && time < afternoonStart -> DayLabel.ThisMorning
    date == today && time < eveningStart -> DayLabel.ThisAfternoon
    date == today -> DayLabel.ThisEvening
    else -> dayLabel(date, today)
  }
}

private fun isLastNight(
  date: LocalDate,
  time: LocalTime,
  today: LocalDate,
): Boolean {
  return (date == today.minusDays(1) && time >= eveningStart) || (date == today && time < nightEnd)
}

private fun dayLabel(
  date: LocalDate,
  today: LocalDate,
): DayLabel {
  return when {
    date == today.minusDays(1) -> DayLabel.Yesterday
    date > today.minusDays(7) && date <= today -> DayLabel.Weekday(date.dayOfWeek)
    else -> DayLabel.Date(date, withYear = date.year != today.year)
  }
}
