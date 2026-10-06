package voice.features.playbackScreen.view

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FormatSeekDeltaTest {

  @Test
  fun `forward seek has plus sign`() {
    assertEquals("+0:40:09", formatSeekDelta(40.minutes + 9.seconds, duration = 9.hours))
  }

  @Test
  fun `backward seek has minus sign`() {
    assertEquals("-1:02:41", formatSeekDelta(-(1.hours + 2.minutes + 41.seconds), duration = 9.hours))
  }

  @Test
  fun `no movement is shown as plus zero`() {
    assertEquals("+00:00", formatSeekDelta(0.seconds, duration = 30.minutes))
  }
}
