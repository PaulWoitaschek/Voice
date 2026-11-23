package voice.features.bookOverview.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.ui.VoiceTheme

@RunWith(AndroidJUnit4::class)
class IdleSearchTest {

  @get:Rule val compose = createComposeRule()

  private val looksThrough =
    "Search looks through titles, authors, narrators, series, genres and chapters."

  @Test
  fun `opening search without a recent search shows what search looks through`() {
    show(recentSearches = emptyList())

    compose.onNodeWithText(looksThrough).assertIsDisplayed()
    compose.onNodeWithText("Recent").assertDoesNotExist()
  }

  @Test
  fun `opening search with recent searches shows only Recent`() {
    show(recentSearches = listOf("herbert", "expanse"))

    compose.onNodeWithText("Recent").assertIsDisplayed()
    compose.onNodeWithText("herbert").assertIsDisplayed()
    compose.onNodeWithText("expanse").assertIsDisplayed()
    compose.onNodeWithText("Browse").assertDoesNotExist()
    compose.onNodeWithText(looksThrough).assertDoesNotExist()
  }

  private fun show(recentSearches: List<String>) {
    compose.setContent {
      VoiceTheme {
        IdleSearch(
          viewState = BookSearchViewState.Idle(recentSearches),
          contentPadding = PaddingValues(),
          listener = BookSearchListener.Noop,
        )
      }
    }
  }
}
