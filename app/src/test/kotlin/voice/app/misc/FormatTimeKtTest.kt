package voice.app.misc

import voice.core.ui.formatTime
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTimeKtTest {

  @Test
  fun withSeconds_noDuration_no_hours_multipleDigits() {
    val ms = ms(hours = 0, minutes = 59, seconds = 12)
    val formatted = formatTime(ms)
    assertEquals(expected = "59:12", actual = formatted)
  }

  @Test
  fun withSeconds_noDuration_no_hours_singleDigit() {
    val ms = ms(hours = 0, minutes = 5, seconds = 7)
    val formatted = formatTime(ms)
    assertEquals(expected = "5:07", actual = formatted)
  }

  @Test
  fun withSeconds_noDuration_with_hours_multipleDigits() {
    val ms = ms(hours = 123, minutes = 59, seconds = 12)
    val formatted = formatTime(ms)
    assertEquals(expected = "123:59:12", actual = formatted)
  }

  @Test
  fun withDuration_andSeconds() {
    val durationMs = ms(hours = 999, minutes = 59, seconds = 12)
    val time = ms(0, 12, 13)
    val formatted = formatTime(time, durationMs)
    assertEquals(expected = "000:12:13", actual = formatted)
  }

  @Test
  fun zero() {
    val formatted = formatTime(0)
    assertEquals(expected = "0:00", actual = formatted)
  }

  private fun ms(
    hours: Long,
    minutes: Long,
    seconds: Long,
  ): Long {
    return TimeUnit.HOURS.toMillis(hours) +
      TimeUnit.MINUTES.toMillis(minutes) +
      TimeUnit.SECONDS.toMillis(seconds)
  }
}
