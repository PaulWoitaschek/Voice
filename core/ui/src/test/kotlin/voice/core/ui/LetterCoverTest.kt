package voice.core.ui

import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LetterCoverTest {

  @Test
  fun `a book without a cover shows the first letter of its title`() {
    assertEquals(expected = "D", actual = letterCover("Dune", dark = false)?.letter)
  }

  @Test
  fun `the letter is a capital`() {
    assertEquals(expected = "D", actual = letterCover("dune", dark = false)?.letter)
  }

  @Test
  fun `a title that starts with a digit shows the digit`() {
    assertEquals(expected = "1", actual = letterCover("1984", dark = false)?.letter)
  }

  @Test
  fun `signs before the first letter are skipped`() {
    assertEquals(expected = "Å", actual = letterCover("„åsa“", dark = false)?.letter)
  }

  @Test
  fun `a title without a letter or digit has no letter cover`() {
    assertNull(letterCover("…", dark = false))
  }

  @Test
  fun `the same title always gets the same color`() {
    assertEquals(
      expected = letterCover("Dune", dark = true),
      actual = letterCover("Dune", dark = true),
    )
  }

  @Test
  fun `Dune and Dracula get different colors`() {
    assertNotEquals(
      illegal = letterCover("Dune", dark = false)?.background,
      actual = letterCover("Dracula", dark = false)?.background,
    )
  }

  @Test
  fun `titles spread over the colors`() {
    val colors =
      listOf("Dune", "Dracula", "Emma", "Ulysses", "Beloved", "Middlemarch", "Hamlet", "Walden")
        .map { letterCover(it, dark = false)!!.background }
        .toSet()
    assertTrue(colors.size >= 5)
  }

  @Test
  fun `the colors follow the light and the dark theme`() {
    val light = letterCover("Dune", dark = false)!!
    val dark = letterCover("Dune", dark = true)!!
    assertTrue(light.background.luminance() > dark.background.luminance())
    assertTrue(light.content.luminance() < dark.content.luminance())
  }
}
