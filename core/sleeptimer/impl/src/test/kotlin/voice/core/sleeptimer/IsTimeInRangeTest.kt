package voice.core.sleeptimer

import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.LocalTime

class IsTimeInRangeTest {
  @Test
  fun rangeWrapsAroundMidnight() {
    val startTime = LocalTime.of(22, 0)
    val endTime = LocalTime.of(6, 0)
    isTimeInRange(LocalTime.of(5, 59), startTime, endTime) shouldBe true
    isTimeInRange(LocalTime.of(6, 0), startTime, endTime) shouldBe false
    isTimeInRange(LocalTime.of(12, 30), startTime, endTime) shouldBe false
    isTimeInRange(LocalTime.of(22, 0), startTime, endTime) shouldBe false
    isTimeInRange(LocalTime.of(22, 1), startTime, endTime) shouldBe true
  }

  @Test
  fun startAndEndOnSameDay() {
    val startTime = LocalTime.of(6, 0)
    val endTime = LocalTime.of(22, 0)
    isTimeInRange(LocalTime.of(5, 59), startTime, endTime) shouldBe false
    isTimeInRange(LocalTime.of(6, 1), startTime, endTime) shouldBe true
    isTimeInRange(LocalTime.of(12, 5), startTime, endTime) shouldBe true
    isTimeInRange(LocalTime.of(22, 0), startTime, endTime) shouldBe false
  }

  @Test
  fun invalidRange() {
    val startTime = LocalTime.of(6, 0)
    val endTime = LocalTime.of(6, 0)
    isTimeInRange(LocalTime.of(6, 0), startTime, endTime) shouldBe false
    isTimeInRange(LocalTime.of(6, 1), startTime, endTime) shouldBe false
    isTimeInRange(LocalTime.of(12, 5), startTime, endTime) shouldBe false
  }
}
