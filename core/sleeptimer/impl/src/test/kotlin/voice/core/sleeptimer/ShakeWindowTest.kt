package voice.core.sleeptimer

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class ShakeWindowTest {

  private val window = ShakeWindow()

  @Test
  fun `shake is detected once accelerating samples span the minimum window`() {
    assertFalse(window.add(0.milliseconds, accelerating = true))
    assertFalse(window.add(100.milliseconds, accelerating = true))
    assertFalse(window.add(200.milliseconds, accelerating = true))
    assertTrue(window.add(250.milliseconds, accelerating = true))
  }

  @Test
  fun `no shake when the samples span less than the minimum window`() {
    repeat(10) {
      assertFalse(window.add((it * 20).milliseconds, accelerating = true))
    }
  }

  @Test
  fun `no shake when too few samples are accelerating`() {
    assertFalse(window.add(0.milliseconds, accelerating = true))
    assertFalse(window.add(100.milliseconds, accelerating = false))
    assertFalse(window.add(200.milliseconds, accelerating = false))
    assertFalse(window.add(300.milliseconds, accelerating = true))
  }

  @Test
  fun `samples older than the maximum window are dropped`() {
    repeat(10) {
      assertFalse(window.add((it * 20).milliseconds, accelerating = false))
    }
    assertFalse(window.add(1000.milliseconds, accelerating = true))
    assertFalse(window.add(1100.milliseconds, accelerating = true))
    assertTrue(window.add(1200.milliseconds, accelerating = true))
  }
}
