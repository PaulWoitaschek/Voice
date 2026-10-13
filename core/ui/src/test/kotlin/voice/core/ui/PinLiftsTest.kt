package voice.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class PinLiftsTest {

  @Test
  fun `pins with room stay on the bar`() {
    assertEquals(expected = listOf(0, 0, 0), actual = pinLifts(listOf(10F, 22F, 100F), pinWidth = 12F))
  }

  @Test
  fun `a pin too close to the one before goes up a row`() {
    assertEquals(expected = listOf(0, 1), actual = pinLifts(listOf(10F, 21F), pinWidth = 12F))
  }

  @Test
  fun `a pin goes back down as soon as the bottom row has room`() {
    assertEquals(
      expected = listOf(0, 1, 0, 1, 0),
      actual = pinLifts(listOf(10F, 15F, 25F, 30F, 40F), pinWidth = 12F),
    )
  }

  @Test
  fun `once every row is taken a pin goes where it overlaps least`() {
    assertEquals(
      expected = listOf(0, 1, 0),
      actual = pinLifts(listOf(10F, 12F, 14F), pinWidth = 12F),
    )
  }

  @Test
  fun `lifts follow the position, not the order of the pins`() {
    assertEquals(expected = listOf(1, 0), actual = pinLifts(listOf(15F, 10F), pinWidth = 12F))
  }
}
