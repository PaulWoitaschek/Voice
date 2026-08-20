package voice.features.listeningStats

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Period bounds are the core domain logic of the stats page and must cover natural days, week starts, and DST switches.
class ListeningStatsPeriodTest {

  // Remember the initial default locale so tests never leak into each other.
  private var originalLocale: Locale? = null

  @AfterTest
  fun restoreLocale() {
    originalLocale?.let { Locale.setDefault(it) }
  }

  // A fixed noon wall clock on Jan 15 verifies day bounds start at local midnight.
  @Test
  fun `day range covers local calendar day`() {
    val clock = clock("2024-01-15T12:00:00Z", "Europe/Berlin")
    val range = ListeningStatsPeriod.Day.range(clock.instant(), clock.zone)
    // Berlin Jan 15 00:00 = UTC Jan 14 23:00.
    assertEquals(expected = "2024-01-14T23:00:00Z", actual = range.startedAt.toString())
    // Berlin Jan 16 00:00 = UTC Jan 15 23:00.
    assertEquals(expected = "2024-01-15T23:00:00Z", actual = range.endedAt.toString())
  }

  // US calendars start the week on Sunday; Jan 15 2024 is a Monday, so the range must go back to Jan 14.
  @Test
  fun `week range starts on sunday for US locale`() {
    originalLocale = Locale.getDefault()
    Locale.setDefault(Locale.US)
    val clock = clock("2024-01-15T12:00:00Z", "UTC")
    val range = ListeningStatsPeriod.Week.range(clock.instant(), clock.zone)
    assertEquals(expected = "2024-01-14T00:00:00Z", actual = range.startedAt.toString())
    assertEquals(expected = "2024-01-21T00:00:00Z", actual = range.endedAt.toString())
  }

  // German calendars start the week on Monday; Jan 15 2024 happens to be a Monday.
  @Test
  fun `week range starts on monday for german locale`() {
    originalLocale = Locale.getDefault()
    Locale.setDefault(Locale.GERMANY)
    val clock = clock("2024-01-15T12:00:00Z", "UTC")
    val range = ListeningStatsPeriod.Week.range(clock.instant(), clock.zone)
    assertEquals(expected = "2024-01-15T00:00:00Z", actual = range.startedAt.toString())
    assertEquals(expected = "2024-01-22T00:00:00Z", actual = range.endedAt.toString())
  }

  // Feb 10 aggregates segments from Feb 1 to Mar 1.
  @Test
  fun `month range covers natural month`() {
    val clock = clock("2024-02-10T08:00:00Z", "UTC")
    val range = ListeningStatsPeriod.Month.range(clock.instant(), clock.zone)
    assertEquals(expected = "2024-02-01T00:00:00Z", actual = range.startedAt.toString())
    assertEquals(expected = "2024-03-01T00:00:00Z", actual = range.endedAt.toString())
  }

  // Berlin switches to DST at 02:00 on Mar 31, so that day has only 23 hours and bounds must stay correct.
  @Test
  fun `day range survives dst switch`() {
    val clock = clock("2024-03-31T12:00:00Z", "Europe/Berlin")
    val range = ListeningStatsPeriod.Day.range(clock.instant(), clock.zone)
    // Berlin Mar 31 00:00 is still winter time (UTC+1), i.e. UTC Mar 30 23:00.
    assertEquals(expected = "2024-03-30T23:00:00Z", actual = range.startedAt.toString())
    // Berlin Apr 1 00:00 is already DST (UTC+2), i.e. UTC Mar 31 22:00, making the day 23 hours.
    assertEquals(expected = "2024-03-31T22:00:00Z", actual = range.endedAt.toString())
  }

  // A fixed test clock keeps assertions independent of real time.
  private fun clock(
    instant: String,
    zone: String,
  ): Clock {
    return Clock.fixed(Instant.parse(instant), ZoneId.of(zone))
  }
}
