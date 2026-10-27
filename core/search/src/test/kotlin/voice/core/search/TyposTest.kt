package voice.core.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TyposTest {

  @Test
  fun `one wrong, missing, extra or swapped character`() {
    assertEquals(expected = 6, actual = "potter".typoPrefixLength("pottar"))
    assertEquals(expected = 6, actual = "potter".typoPrefixLength("pottr"))
    assertEquals(expected = 6, actual = "potter".typoPrefixLength("pottter"))
    assertEquals(expected = 6, actual = "potter".typoPrefixLength("pottre"))
  }

  @Test
  fun `the typo can be in the start of a longer word`() {
    assertEquals(expected = 4, actual = "sanderson".typoPrefixLength("sanf"))
    assertEquals(expected = 7, actual = "sanderson".typoPrefixLength("sandrs"))
  }

  @Test
  fun `two typos are too many`() {
    assertNull("potter".typoPrefixLength("pottra"))
    assertNull("potter".typoPrefixLength("ptoetr"))
    assertNull("pot".typoPrefixLength("potter"))
  }
}
