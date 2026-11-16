package voice.features.review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.serialization.json.Json

class ReviewPromptStateTest {

  @Test
  fun `state stored as plain numbers is read the same`() {
    val stored =
      """{"asks":1,"lastAskedAtMillis":1000,"listenedAtLastAskMillis":3600000,""" +
        """"celebratedMilestoneHours":5,"snoozedUntilMillis":2000}"""

    val state = Json.decodeFromString(ReviewPromptState.serializer(), stored)

    assertEquals(
      expected =
        ReviewPromptState(
          asks = 1,
          lastAskedAt = Instant.fromEpochMilliseconds(1000),
          listenedAtLastAsk = 1.hours,
          celebratedMilestone = 5.hours,
          snoozedUntil = Instant.fromEpochMilliseconds(2000),
        ),
      actual = state,
    )
    assertEquals(
      expected = stored,
      actual = Json.encodeToString(ReviewPromptState.serializer(), state),
    )
  }
}
