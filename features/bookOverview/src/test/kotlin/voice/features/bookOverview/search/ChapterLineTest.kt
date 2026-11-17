package voice.features.bookOverview.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import voice.core.data.BookCategory
import voice.core.data.BookId
import voice.core.search.BookSearchField
import voice.core.ui.VoiceTheme

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChapterLineTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun `a chapter name that fits shows in full`() {
    setContent(HighlightedText("The Red-Headed League", listOf(15..20)))

    assertEquals(expected = "The Red-Headed League", actual = chapterLine().substringAfter("· "))
  }

  @Test
  fun `a long chapter name drops its first words so the matched word shows`() {
    val name = "Harry Potter and the Chamber of Secrets - Chapter 17 - The Heir of Slytherin"
    setContent(HighlightedText(name, listOf(67..75)))

    val line = visibleChapterLine()
    assertTrue(line.contains("…") && line.endsWith("Slytherin"), line)
  }

  @Test
  fun `with a large font the matched word still shows`() {
    setContent(HighlightedText("The Red-Headed League", listOf(15..20)), fontScale = 2F)

    val line = visibleChapterLine()
    assertTrue(line.endsWith("League"), line)
  }

  @Test
  fun `the cut keeps a few words before the match, or starts at the match`() {
    assertEquals(
      expected = HighlightedText("…Heir of Slytherin", listOf(9..17)),
      actual =
        HighlightedText(
            "Harry Potter and the Chamber of Secrets - Chapter 17 - The Heir of Slytherin",
            listOf(67..75),
          )
          .nearFirstHighlight(lead = 12),
    )
    assertEquals(
      expected = HighlightedText("…Red Headed", listOf(1..3, 5..10)),
      actual =
        HighlightedText("Introduction-to-the-Methodology Red Headed", listOf(32..34, 36..41))
          .nearFirstHighlight(lead = 0),
    )
    assertEquals(
      expected = HighlightedText("Short Name", listOf(6..9)),
      actual = HighlightedText("Short Name", listOf(6..9)).nearFirstHighlight(lead = 12),
    )
  }

  private fun chapterNode() =
    compose
      .onAllNodesWithText("Chapter ·", substring = true, useUnmergedTree = true)
      .fetchSemanticsNodes()
      .single()

  private fun chapterLine(): String =
    chapterNode().config[SemanticsProperties.Text].joinToString { it.text }

  // the text up to the ellipsis, which is what the listener sees
  private fun visibleChapterLine(): String {
    val layouts = mutableListOf<TextLayoutResult>()
    val _ = chapterNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
    val layout = layouts.single()
    val end = layout.getLineEnd(layout.lineCount - 1, visibleEnd = true)
    return layout.layoutInput.text.text.substring(0, end)
  }

  private fun setContent(
    chapter: HighlightedText,
    fontScale: Float = 1F,
  ) {
    compose.setContent {
      val density = LocalDensity.current
      CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        VoiceTheme {
          SearchResults(
            viewState =
              BookSearchViewState.Results(
                filters = emptyList(),
                topResult = result("Top", null),
                topResultPlaying = false,
                otherResults = listOf(result("Sherlock Holmes", chapter)),
              ),
            contentPadding = PaddingValues(),
            listener = mockk(relaxed = true),
            modifier = Modifier.width(360.dp),
          )
        }
      }
    }
  }

  private fun result(
    title: String,
    chapter: HighlightedText?,
  ) =
    SearchResultViewState(
      id = BookId(title),
      title = HighlightedText(title),
      author = HighlightedText("Arthur Conan Doyle"),
      cover = null,
      category = BookCategory.NOT_STARTED,
      progress = 0F,
      remainingTime = "1:00:00",
      matchTag = chapter?.let { MatchTag(BookSearchField.Chapters, it) },
    )
}
