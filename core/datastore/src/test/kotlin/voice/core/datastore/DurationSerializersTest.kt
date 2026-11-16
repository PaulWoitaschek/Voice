package voice.core.datastore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json

class DurationSerializersTest {

  @Test
  fun `seconds stay the plain number older stores wrote`() {
    assertEquals(
      expected = "20",
      actual = Json.encodeToString(DurationInSecondsSerializer, 20.seconds),
    )
    assertEquals(
      expected = 20.seconds,
      actual = Json.decodeFromString(DurationInSecondsSerializer, "20"),
    )
  }

  @Test
  fun `milliseconds stay the plain number older stores wrote`() {
    assertEquals(
      expected = 1500.milliseconds,
      actual = Json.decodeFromString(DurationInMillisecondsSerializer, "1500"),
    )
  }

  @Test
  fun `hours stay the plain number older stores wrote`() {
    assertEquals(expected = "5", actual = Json.encodeToString(DurationInHoursSerializer, 5.hours))
    assertEquals(expected = 5.hours, actual = Json.decodeFromString(DurationInHoursSerializer, "5"))
  }
}
