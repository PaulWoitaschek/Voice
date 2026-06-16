package voice.features.settings.views.sleeptimer

import java.time.LocalTime
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalTimeFormatterTest {

  @Test
  fun `localTimeFormatter for 24-hour format`() {
    val formatter = localTimeFormatter(is24HourFormat = true, Locale.US)

    assertEquals(expected = "00:00", actual = formatter.format(LocalTime.MIDNIGHT))
    assertEquals(expected = "12:00", actual = formatter.format(LocalTime.NOON))
    assertEquals(expected = "13:30", actual = formatter.format(LocalTime.of(13, 30)))
    assertEquals(expected = "08:05", actual = formatter.format(LocalTime.of(8, 5)))
  }

  @Test
  fun `localTimeFormatter for 12-hour format`() {
    val formatter = localTimeFormatter(is24HourFormat = false, Locale.US)

    assertEquals(expected = "12:00 AM", actual = formatter.format(LocalTime.MIDNIGHT))
    assertEquals(expected = "12:00 PM", actual = formatter.format(LocalTime.NOON))
    assertEquals(expected = "01:30 PM", actual = formatter.format(LocalTime.of(13, 30)))
    assertEquals(expected = "08:05 AM", actual = formatter.format(LocalTime.of(8, 5)))
    assertEquals(expected = "11:59 PM", actual = formatter.format(LocalTime.of(23, 59)))
    assertEquals(expected = "01:00 AM", actual = formatter.format(LocalTime.of(1, 0)))
  }
}
