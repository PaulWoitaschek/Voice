package voice.core.scanner

import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(JUnit4::class)
class ParseVorbisDurationTest {

  @Test
  fun `parse standard format duration`() {
    assertEquals(expected = 1.hours + 30.minutes + 45.5.seconds, actual = assertNotNull(parseVorbisDuration("01:30:45.5")))
  }

  @Test
  fun `parse zero values duration`() {
    assertEquals(expected = Duration.ZERO, actual = assertNotNull(parseVorbisDuration("00:00:00.0")))
  }

  @Test
  fun `parse large values duration`() {
    assertEquals(expected = 99.hours + 59.minutes + 59.9.seconds, actual = assertNotNull(parseVorbisDuration("99:59:59.9")))
  }

  @Test
  fun `parse duration without decimal seconds`() {
    assertEquals(expected = 1.hours + 23.minutes + 45.seconds, actual = assertNotNull(parseVorbisDuration("01:23:45")))
  }

  @Test
  fun `invalid duration with wrong number of components`() {
    assertNull(parseVorbisDuration("01:30"))
    assertNull(parseVorbisDuration("01:30:45:10"))
  }

  @Test
  fun `invalid duration with non-numeric values`() {
    assertNull(parseVorbisDuration("aa:bb:cc"))
    assertNull(parseVorbisDuration("01:aa:45"))
    assertNull(parseVorbisDuration("01:30:cc"))
  }

  @Test
  fun `invalid duration with empty string`() {
    assertNull(parseVorbisDuration(""))
  }

  @Test
  fun `invalid duration with malformed input`() {
    assertNull(parseVorbisDuration("01-30-45"))
    assertNull(parseVorbisDuration("01::30:45"))
  }
}
