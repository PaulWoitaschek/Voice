package voice.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class PinLiftsTest {

  @Test
  fun `pins far apart stay on the bar`() {
    assertEquals(expected = listOf(0, 0, 0), actual = pinLifts(listOf(0.1F, 0.5F, 0.9F)))
  }

  @Test
  fun `pins close to each other stack up to a limit`() {
    assertEquals(
      expected = listOf(0, 1, 2, 2, 0),
      actual = pinLifts(listOf(0.30F, 0.31F, 0.32F, 0.33F, 0.6F)),
    )
  }

  @Test
  fun `lifts follow the position, not the order of the pins`() {
    assertEquals(expected = listOf(1, 0), actual = pinLifts(listOf(0.51F, 0.5F)))
  }
}
