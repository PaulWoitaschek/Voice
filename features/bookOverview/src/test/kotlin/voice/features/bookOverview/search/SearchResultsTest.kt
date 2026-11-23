package voice.features.bookOverview.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.data.BookCategory
import voice.core.data.BookId
import voice.core.ui.VoiceTheme

@RunWith(AndroidJUnit4::class)
class SearchResultsTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun `the rows show Author 7 books and Series 4 books between the first result and the books`() {
    compose.setContent {
      VoiceTheme {
        SearchResults(
          viewState =
            BookSearchViewState.Results(
              topResult = result("Dune"),
              topResultPlaying = false,
              groups =
                listOf(
                  group(SearchGroupViewState.Kind.Author, "Frank Herbert", bookCount = 7),
                  group(SearchGroupViewState.Kind.Series, "The Expanse", bookCount = 4),
                ),
              otherResults = listOf(result("Children of Dune")),
            ),
          contentPadding = PaddingValues(),
          listener = BookSearchListener.Noop,
        )
      }
    }

    compose.onNodeWithText("Author · 7 books").assertIsDisplayed()
    compose.onNodeWithText("Series · 4 books").assertIsDisplayed()
    val tops =
      listOf("Dune", "Frank Herbert", "The Expanse", "Children of Dune").map {
        compose.onNodeWithText(it).getBoundsInRoot().top
      }
    assertEquals(expected = tops.sorted(), actual = tops)
  }

  private fun result(title: String) =
    SearchResultViewState(
      id = BookId(title),
      title = HighlightedText(title),
      author = null,
      cover = null,
      category = BookCategory.NOT_STARTED,
      progress = 0F,
      remainingTime = "1:00:00",
      matchTag = null,
    )

  private fun group(
    kind: SearchGroupViewState.Kind,
    name: String,
    bookCount: Int,
  ) =
    SearchGroupViewState(
      kind = kind,
      key = name,
      name = name,
      bookCount = bookCount,
      covers = listOf(null, null),
    )
}
