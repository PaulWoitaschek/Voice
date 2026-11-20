package voice.features.bookOverview.views

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.ui.VoiceTheme

@RunWith(AndroidJUnit4::class)
class LibraryGreetingTest {

  @get:Rule val compose = createComposeRule()

  private var clicks = 0

  @Test
  fun `library, 2 books in progress, 6 h 40 min listened this week, open the library, the line adds the time of this week`() {
    setContent(inProgressCount = 2, listenedThisWeek = 6.hours + 40.minutes)

    compose
      .onNodeWithText("2 books in progress · 6h 40m this week")
      .assertHasClickAction()
      .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
      .assertHeightIsAtLeast(48.dp)
  }

  @Test
  fun `library, no listening this week, some before, open the library, the same text, and a tap opens the screen`() {
    setContent(inProgressCount = 2, listenedThisWeek = Duration.ZERO)

    compose.onNodeWithText("2 books in progress").performClick()

    assertEquals(expected = 1, actual = clicks)
  }

  @Test
  fun `library, no book in progress, 1 h listened this week, open the library, 1 h this week`() {
    setContent(inProgressCount = 0, listenedThisWeek = 1.hours)

    compose.onNodeWithText("1h this week").assertHasClickAction()
  }

  @Test
  fun `library, no book in progress, no listening this week, some before, open the library, 0 min this week, and a tap opens the screen`() {
    setContent(inProgressCount = 0, listenedThisWeek = Duration.ZERO)

    compose.onNodeWithText("0m this week").performClick()

    assertEquals(expected = 1, actual = clicks)
  }

  @Test
  fun `library, no book in progress, Voice never counted any listening, open the library, no line`() {
    setContent(inProgressCount = 0, listenedThisWeek = null)

    compose.onNodeWithText("this week", substring = true).assertDoesNotExist()
    compose.onNodeWithText("in progress", substring = true).assertDoesNotExist()
  }

  @Test
  fun `library, Voice never counted any listening, tap the line, nothing`() {
    setContent(inProgressCount = 2, listenedThisWeek = null)

    compose.onNodeWithText("2 books in progress").assertHasNoClickAction()
  }

  private fun setContent(
    inProgressCount: Int,
    listenedThisWeek: Duration?,
  ) {
    compose.setContent {
      VoiceTheme {
        LibraryGreeting(
          inProgressCount = inProgressCount,
          listenedThisWeek = listenedThisWeek,
          onListeningClick = { clicks++ },
        )
      }
    }
  }
}
