package voice.core.sleeptimer

import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class IsTimeInRangeTest {
  @Test
  fun rangeWrapsAroundMidnight() {
    val startTime = LocalTime.of(22, 0)
    val endTime = LocalTime.of(6, 0)
    assertEquals(expected = true, actual = isTimeInRange(LocalTime.of(5, 59), startTime, endTime))
    assertEquals(expected = false, actual = isTimeInRange(LocalTime.of(6, 0), startTime, endTime))
    assertEquals(expected = false, actual = isTimeInRange(LocalTime.of(12, 30), startTime, endTime))
    assertEquals(expected = false, actual = isTimeInRange(LocalTime.of(22, 0), startTime, endTime))
    assertEquals(expected = true, actual = isTimeInRange(LocalTime.of(22, 1), startTime, endTime))
  }

  @Test
  fun startAndEndOnSameDay() {
    val startTime = LocalTime.of(6, 0)
    val endTime = LocalTime.of(22, 0)
    assertEquals(expected = false, actual = isTimeInRange(LocalTime.of(5, 59), startTime, endTime))
    assertEquals(expected = true, actual = isTimeInRange(LocalTime.of(6, 1), startTime, endTime))
    assertEquals(expected = true, actual = isTimeInRange(LocalTime.of(12, 5), startTime, endTime))
    assertEquals(expected = false, actual = isTimeInRange(LocalTime.of(22, 0), startTime, endTime))
  }

  @Test
  fun invalidRange() {
    val startTime = LocalTime.of(6, 0)
    val endTime = LocalTime.of(6, 0)
    assertEquals(expected = false, actual = isTimeInRange(LocalTime.of(6, 0), startTime, endTime))
    assertEquals(expected = false, actual = isTimeInRange(LocalTime.of(6, 1), startTime, endTime))
    assertEquals(expected = false, actual = isTimeInRange(LocalTime.of(12, 5), startTime, endTime))
  }
}
