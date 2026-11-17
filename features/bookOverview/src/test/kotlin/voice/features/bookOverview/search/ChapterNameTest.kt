package voice.features.bookOverview.search

import kotlin.test.Test
import kotlin.test.assertEquals

class ChapterNameTest {

  @Test
  fun `a long chapter name whose match starts late shows from a few words before the match`() {
    val name =
      "In which our heroes travel very far across the moors and the city at night to meet the clockmaker"

    assertEquals(
      expected = HighlightedText("…meet the clockmaker", listOf(10..19)),
      actual = HighlightedText(name, listOf(87..96)).nearFirstHighlight(),
    )
  }

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
  fun `The Red-Headed League shows in full`() {
    assertEquals(
      expected = HighlightedText("The Red-Headed League", listOf(15..20)),
      actual = HighlightedText("The Red-Headed League", listOf(15..20)).nearFirstHighlight(),
    )
  }
}
