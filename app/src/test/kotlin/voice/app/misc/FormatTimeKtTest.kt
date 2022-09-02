package voice.app.misc

import io.kotest.matchers.shouldBe
import org.junit.Test
import voice.common.formatTime
import java.util.concurrent.TimeUnit

class FormatTimeKtTest {

  @Test
  fun withSeconds_noDuration_no_hours_multipleDigits() {
    val ms = ms(hours = 0, minutes = 59, seconds = 12)
    val formatted = formatTime(ms)
    formatted shouldBe "59:12"
  }

  @Test
  fun withSeconds_noDuration_no_hours_singleDigit() {
    val ms = ms(hours = 0, minutes = 5, seconds = 7)
    val formatted = formatTime(ms)
    formatted shouldBe "5:07"
  }

  @Test
  fun withSeconds_noDuration_with_hours_multipleDigits() {
    val ms = ms(hours = 123, minutes = 59, seconds = 12)
    val formatted = formatTime(ms)
    formatted shouldBe "123:59:12"
  }

  @Test
  fun withDuration_andSeconds() {
    val durationMs = ms(hours = 999, minutes = 59, seconds = 12)
    val time = ms(0, 12, 13)
    val formatted = formatTime(time, durationMs)
    formatted shouldBe "000:12:13"
  }

  @Test
  fun zero() {
    val formatted = formatTime(0)
    formatted shouldBe "0:00"
  }

  private fun ms(hours: Long, minutes: Long, seconds: Long): Long {
    return TimeUnit.HOURS.toMillis(hours) +
      TimeUnit.MINUTES.toMillis(minutes) +
      TimeUnit.SECONDS.toMillis(seconds)
  }
}
