package voice.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class BookBarXTest {

  private fun x(
    fraction: Float,
    segments: List<Float>,
    width: Float,
  ) = bookBarX(fraction, segments, width, gap = 3F, minSegmentWidth = 3F)

  @Test
  fun `a spot sits where its chapter is drawn, after the gaps before it`() {
    // 100 px for the segments and 10 gaps of 3 px: one long chapter, then ten short ones
    val segments = listOf(0.5F) + List(10) { 0.05F }
    // half way into the long chapter, without any gap before it
    assertEquals(expected = 25F, actual = x(0.25F, segments, width = 130F), absoluteTolerance = 0.01F)
    // half way into the last chapter, after all ten gaps
    assertEquals(expected = 127.5F, actual = x(0.975F, segments, width = 130F), absoluteTolerance = 0.01F)
  }

  @Test
  fun `the start of a chapter is the start of its segment`() {
    val segments = listOf(0.5F, 0.5F)
    assertEquals(expected = 53F, actual = x(0.5F, segments, width = 103F), absoluteTolerance = 0.01F)
  }

  @Test
  fun `the ends of the book are the ends of the bar`() {
    val segments = listOf(0.2F, 0.3F, 0.5F)
    assertEquals(expected = 0F, actual = x(0F, segments, width = 106F))
    assertEquals(expected = 106F, actual = x(1F, segments, width = 106F), absoluteTolerance = 0.01F)
    assertEquals(expected = 106F, actual = x(1.2F, segments, width = 106F))
    assertEquals(expected = 0F, actual = x(-0.1F, segments, width = 106F))
  }

  @Test
  fun `a chapter without length is skipped`() {
    val segments = listOf(0.5F, 0F, 0.5F)
    // the empty segment still takes its gaps, so the next chapter starts after two of them
    assertEquals(expected = 56F, actual = x(0.5F, segments, width = 106F), absoluteTolerance = 0.01F)
  }

  @Test
  fun `a single chapter is a plain bar`() {
    assertEquals(expected = 30F, actual = x(0.3F, listOf(1F), width = 100F), absoluteTolerance = 0.01F)
    assertEquals(expected = 30F, actual = x(0.3F, emptyList(), width = 100F), absoluteTolerance = 0.01F)
  }

  @Test
  fun `segments too thin to draw make a plain bar`() {
    // 20 segments with 3 px gaps leave less than 3 px for each on a 100 px bar
    val segments = List(20) { 0.05F }
    assertEquals(expected = 50F, actual = x(0.5F, segments, width = 100F), absoluteTolerance = 0.01F)
  }
}
