package voice.settings.views.sleeptimer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.util.Locale

class LocalTimeFormatterTest {

  @Test
  fun `localTimeFormatter for 24-hour format`() {
    val formatter = localTimeFormatter(is24HourFormat = true, Locale.US)

    assertEquals("00:00", formatter.format(LocalTime.MIDNIGHT))
    assertEquals("12:00", formatter.format(LocalTime.NOON))
    assertEquals("13:30", formatter.format(LocalTime.of(13, 30)))
    assertEquals("08:05", formatter.format(LocalTime.of(8, 5)))
  }

  @Test
  fun `localTimeFormatter for 12-hour format`() {
    val formatter = localTimeFormatter(is24HourFormat = false, Locale.US)

    assertEquals("12:00 AM", formatter.format(LocalTime.MIDNIGHT))
    assertEquals("12:00 PM", formatter.format(LocalTime.NOON))
    assertEquals("01:30 PM", formatter.format(LocalTime.of(13, 30)))
    assertEquals("08:05 AM", formatter.format(LocalTime.of(8, 5)))
    assertEquals("11:59 PM", formatter.format(LocalTime.of(23, 59)))
    assertEquals("01:00 AM", formatter.format(LocalTime.of(1, 0)))
  }
}
