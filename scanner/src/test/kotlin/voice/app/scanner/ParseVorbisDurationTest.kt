package voice.app.scanner

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(JUnit4::class)
class ParseVorbisDurationTest {

  @Test
  fun `parse standard format duration`() {
    parseVorbisDuration("01:30:45.5").shouldNotBeNull().shouldBe(
      1.hours + 30.minutes + 45.5.seconds,
    )
  }

  @Test
  fun `parse zero values duration`() {
    parseVorbisDuration("00:00:00.0").shouldNotBeNull().shouldBe(Duration.ZERO)
  }

  @Test
  fun `parse large values duration`() {
    parseVorbisDuration("99:59:59.9").shouldNotBeNull().shouldBe(
      99.hours + 59.minutes + 59.9.seconds,
    )
  }

  @Test
  fun `parse duration without decimal seconds`() {
    parseVorbisDuration("01:23:45").shouldNotBeNull().shouldBe(
      1.hours + 23.minutes + 45.seconds,
    )
  }

  @Test
  fun `invalid duration with wrong number of components`() {
    parseVorbisDuration("01:30").shouldBeNull()
    parseVorbisDuration("01:30:45:10").shouldBeNull()
  }

  @Test
  fun `invalid duration with non-numeric values`() {
    parseVorbisDuration("aa:bb:cc").shouldBeNull()
    parseVorbisDuration("01:aa:45").shouldBeNull()
    parseVorbisDuration("01:30:cc").shouldBeNull()
  }

  @Test
  fun `invalid duration with empty string`() {
    parseVorbisDuration("").shouldBeNull()
  }

  @Test
  fun `invalid duration with malformed input`() {
    parseVorbisDuration("01-30-45").shouldBeNull()
    parseVorbisDuration("01::30:45").shouldBeNull()
  }
}
