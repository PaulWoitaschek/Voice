package voice.features.bookOverview.search

import kotlin.test.Test
import kotlin.test.assertEquals

class HighlightedTextTest {

  @Test
  fun `a chapter name whose match starts early shows in full`() {
    val name =
      "In which our heroes travel very far across the moors and the city at night to meet the clockmaker"

    assertEquals(
      expected = HighlightedText(name, listOf(13..18)),
      actual = HighlightedText(name, listOf(13..18)).nearFirstHighlight(),
    )
  }

  @Test
  fun `the cut moves each highlight of several words`() {
    assertEquals(
      expected = HighlightedText("…far to the Red-Headed League", listOf(12..14, 16..21)),
      actual =
        HighlightedText(
            "In which our heroes travel very far to the Red-Headed League",
            listOf(43..45, 47..52),
          )
          .nearFirstHighlight(),
    )
  }

  @Test
  fun `without a space before the match the cut starts at the match`() {
    assertEquals(
      expected = HighlightedText("…Red-Headed", listOf(1..3, 5..10)),
      actual =
        HighlightedText("Introduction-to-the-Methodology-of-the-Red-Headed", listOf(39..41, 43..48))
          .nearFirstHighlight(),
    )
  }

  @Test
  fun `the cut doesn't start at a separator`() {
    assertEquals(
      expected = HighlightedText("…The Five Orange Pips and Other Stories", listOf(10..15, 17..20)),
      actual =
        HighlightedText(
            "Sherlock Holmes - 05 - The Five Orange Pips and Other Stories",
            listOf(32..37, 39..42),
          )
          .nearFirstHighlight(),
    )
  }

  @Test
  fun `a chapter named like the label highlights the name, not the label`() {
    assertEquals(
      expected = HighlightedText("Chapter · Chapter and Verse", listOf(10..16)),
      actual = HighlightedText("Chapter and Verse", listOf(0..6)).inLabel("Chapter · $PLACEHOLDER"),
    )
  }

  @Test
  fun `a label without the name highlights nothing`() {
    assertEquals(
      expected = HighlightedText("Chapter", emptyList()),
      actual = HighlightedText("Chapter and Verse", listOf(0..6)).inLabel("Chapter"),
    )
  }
}
