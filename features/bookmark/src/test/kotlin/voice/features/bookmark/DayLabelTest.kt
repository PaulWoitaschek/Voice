package voice.features.bookmark

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DayLabelTest {

  private val now = today(10)

  private fun saved(
    at: Instant,
    setBySleepTimer: Boolean = false,
  ) = savedAtLabel(at, now, testZone, setBySleepTimer)

  @Test
  fun `under a minute ago is just now`() {
    assertEquals(expected = DayLabel.JustNow, actual = saved(now.minusSeconds(30)))
  }

  @Test
  fun `earlier today shows the time`() {
    assertEquals(expected = DayLabel.Today(LocalTime.of(8, 15)), actual = saved(today(8, 15)))
  }

  @Test
  fun `a sleep timer bookmark from the night is last night`() {
    val yesterdayLate = ZonedDateTime.of(2026, 10, 6, 23, 42, 0, 0, testZone).toInstant()
    assertEquals(expected = DayLabel.LastNight, actual = saved(yesterdayLate, setBySleepTimer = true))
    assertEquals(expected = DayLabel.LastNight, actual = saved(today(1, 30), setBySleepTimer = true))
  }

  @Test
  fun `other bookmarks from the night are yesterday`() {
    val yesterdayLate = ZonedDateTime.of(2026, 10, 6, 23, 42, 0, 0, testZone).toInstant()
    assertEquals(expected = DayLabel.Yesterday, actual = saved(yesterdayLate))
  }

  @Test
  fun `this week shows the weekday, older the date`() {
    val thursday = ZonedDateTime.of(2026, 10, 1, 12, 0, 0, 0, testZone).toInstant()
    assertEquals(expected = DayLabel.Weekday(DayOfWeek.THURSDAY), actual = saved(thursday))

    val september = ZonedDateTime.of(2026, 9, 28, 12, 0, 0, 0, testZone).toInstant()
    assertEquals(expected = DayLabel.Date(LocalDate.of(2026, 9, 28), withYear = false), actual = saved(september))

    val lastYear = ZonedDateTime.of(2025, 12, 24, 12, 0, 0, 0, testZone).toInstant()
    assertEquals(expected = DayLabel.Date(LocalDate.of(2025, 12, 24), withYear = true), actual = saved(lastYear))
  }

  @Test
  fun `sessions are named by part of the day`() {
    val evening = today(21)
    assertEquals(expected = DayLabel.ThisMorning, actual = sessionLabel(today(7, 52), evening, testZone))
    assertEquals(expected = DayLabel.ThisAfternoon, actual = sessionLabel(today(14), evening, testZone))
    assertEquals(expected = DayLabel.ThisEvening, actual = sessionLabel(today(19), evening, testZone))
    val yesterdayLate = ZonedDateTime.of(2026, 10, 6, 22, 10, 0, 0, testZone).toInstant()
    assertEquals(expected = DayLabel.LastNight, actual = sessionLabel(yesterdayLate, evening, testZone))
  }
}
