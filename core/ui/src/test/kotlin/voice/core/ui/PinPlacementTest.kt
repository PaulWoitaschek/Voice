package voice.core.ui

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import voice.core.data.Bookmark
import kotlin.test.Test
import kotlin.test.assertEquals

class PinPlacementTest {

  private val density = Density(1F)
  private val pinSize = 14.dp

  private fun place(vararg positions: Float) = with(density) {
    placePins(positions.map { BookBarPin(it, Bookmark.Kind.Note) }, segments = listOf(1F), width = 300F, pinSize = pinSize)
  }

  @Test
  fun `pins at the ends stay on the bar`() {
    assertEquals(expected = listOf(7F, 293F), actual = place(0F, 1F).centers)
  }

  @Test
  fun `the bar starts below the rows of pins`() {
    assertEquals(expected = 0F, actual = place().barTop)
    assertEquals(expected = 17F, actual = place(0.5F).barTop)
    assertEquals(expected = 33F, actual = place(0.5F, 0.51F).barTop)
  }

  @Test
  fun `a tap on a stacked pin opens that pin, not the one below`() {
    val placement = place(0.5F, 0.51F)
    val upper = placement.tops.indexOf(placement.tops.min())
    val lower = 1 - upper

    val tapUpper = placement.pinAt(placement.centers[lower], placement.tops[upper] + 7F, pinSize = 14F, reach = 20F)
    val tapLower = placement.pinAt(placement.centers[upper], placement.tops[lower] + 7F, pinSize = 14F, reach = 20F)

    assertEquals(expected = upper, actual = tapUpper)
    assertEquals(expected = lower, actual = tapLower)
  }

  @Test
  fun `a tap far from every pin opens none`() {
    assertEquals(expected = null, actual = place(0.1F).pinAt(250F, 7F, pinSize = 14F, reach = 20F))
  }
}
