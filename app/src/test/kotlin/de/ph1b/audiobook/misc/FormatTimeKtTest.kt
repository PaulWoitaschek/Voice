package de.ph1b.audiobook.misc

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit

class FormatTimeKtTest {

  @Test
  fun withSeconds_noDuration_no_hours_multipleDigits() {
    val ms = ms(hours = 0, minutes = 59, seconds = 12)
    val formatted = formatTime(ms)
    assertThat(formatted).isEqualTo("59:12")
  }

  @Test
  fun withSeconds_noDuration_no_hours_singleDigit() {
    val ms = ms(hours = 0, minutes = 5, seconds = 7)
    val formatted = formatTime(ms)
    assertThat(formatted).isEqualTo("5:07")
  }

  @Test
  fun withSeconds_noDuration_with_hours_multipleDigits() {
    val ms = ms(hours = 123, minutes = 59, seconds = 12)
    val formatted = formatTime(ms)
    assertThat(formatted).isEqualTo("123:59:12")
  }

  @Test
  fun withDuration_andSeconds() {
    val durationMs = ms(hours = 999, minutes = 59, seconds = 12)
    val time = ms(0, 12, 13)
    val formatted = formatTime(time, durationMs)
    assertThat(formatted).isEqualTo("000:12:13")
  }

  @Test
  fun zero() {
    val formatted = formatTime(0)
    assertThat(formatted).isEqualTo("0:00")
  }

  private fun ms(hours: Long, minutes: Long, seconds: Long): Long {
    return TimeUnit.HOURS.toMillis(hours) +
      TimeUnit.MINUTES.toMillis(minutes) +
      TimeUnit.SECONDS.toMillis(seconds)
  }
}
