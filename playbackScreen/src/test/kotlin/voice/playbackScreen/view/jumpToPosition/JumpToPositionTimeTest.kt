package voice.playbackScreen.view.jumpToPosition

import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class JumpToPositionTimeTest {

  @Test
  fun testValidInput() {
    jumpToPositionTime("7654321") shouldBe 765.hours + 43.minutes + 21.seconds
  }

  @Test
  fun testLeadingZeros() {
    jumpToPositionTime("001234") shouldBe 12.minutes + 34.seconds
  }

  @Test
  fun testMinutesAndSecondsOnly() {
    jumpToPositionTime("1234") shouldBe 12.minutes + 34.seconds
  }

  @Test
  fun testSecondsOnly() {
    jumpToPositionTime("45") shouldBe 45.seconds
  }

  @Test
  fun testEmptyString() {
    jumpToPositionTime("") shouldBe 0.hours + 0.minutes + 0.seconds
  }

  @Test
  fun testLessThanSixDigitsButValid() {
    jumpToPositionTime("123") shouldBe 1.minutes + 23.seconds
  }
}
