package voice.core.audiobookshelf

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerVersionTest {

  private val minimum = listOf(2, 26, 0)

  @Test
  fun `newer versions are supported`() {
    assertTrue("2.26.0".isAtLeast(minimum))
    assertTrue("2.37.1".isAtLeast(minimum))
    assertTrue("3.0.0".isAtLeast(minimum))
    assertTrue("v2.30.0-beta".isAtLeast(minimum))
  }

  @Test
  fun `older versions are not`() {
    assertFalse("2.25.9".isAtLeast(minimum))
    assertFalse("1.40.0".isAtLeast(minimum))
    assertFalse("".isAtLeast(minimum))
  }
}
