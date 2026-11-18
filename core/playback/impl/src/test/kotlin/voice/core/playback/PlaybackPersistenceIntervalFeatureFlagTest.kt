package voice.core.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import voice.core.featureflag.MemoryFeatureFlag

class PlaybackPersistenceIntervalFeatureFlagTest {

  @Test
  fun readsSeconds() {
    assertEquals(120.seconds, interval("120"))
  }

  @Test
  fun fallsBackToThirtySecondsOnInvalidValues() {
    listOf("", "abc", "0", "-5").forEach {
      assertEquals(30.seconds, interval(it), it)
    }
  }

  private fun interval(value: String): Duration =
    PlaybackPersistenceIntervalFeatureFlag(MemoryFeatureFlag(value)).interval()
}
