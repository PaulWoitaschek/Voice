package voice.features.playbackScreen.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import voice.core.ui.VoiceTheme
import voice.features.playbackScreen.BookPlayDialogViewState.SelectChapterDialog
import voice.features.playbackScreen.ChapterSheet

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChapterSheetTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun `the open chapter list follows playback into the next chapter`() {
    var currentChapterNumber by mutableIntStateOf(1)
    compose.setContent {
      VoiceTheme {
        ChapterSheet(
          dialogState =
            SelectChapterDialog(
              listOf(
                SelectChapterDialog.ItemViewState(number = 1, name = "Arrival", time = "0:00"),
                SelectChapterDialog.ItemViewState(number = 2, name = "Departure", time = "2:00"),
              ),
            ),
          currentChapterNumber = currentChapterNumber,
          playing = false,
          onChapterClick = {},
          onDismiss = {},
        )
      }
    }
    compose.onNodeWithText("Arrival").assertIsSelected()
    compose.onNodeWithText("1 / 2").assertExists()

    currentChapterNumber = 2

    compose.onNodeWithText("Departure").assertIsSelected()
    compose.onNodeWithText("Arrival").assertIsNotSelected()
    compose.onNodeWithText("2 / 2").assertExists()
  }

  @Test
  fun `chapter list, a long name shows the whole name`() {
    showChapter(LONG_NAME)

    val layout = compose.onNodeWithText(LONG_NAME, useUnmergedTree = true).textLayout()
    assertTrue(layout.lineCount > 3)
    assertFalse(layout.hasVisualOverflow)
  }

  @Test
  fun `chapter list, a short name stays on one line`() {
    showChapter("Arrival")

    val layout = compose.onNodeWithText("Arrival", useUnmergedTree = true).textLayout()
    assertEquals(1, layout.lineCount)
  }

  private fun showChapter(name: String) {
    compose.setContent {
      VoiceTheme {
        ChapterSheet(
          dialogState =
            SelectChapterDialog(
              listOf(
                SelectChapterDialog.ItemViewState(number = 1, name = "Prologue", time = "0:00"),
                SelectChapterDialog.ItemViewState(number = 2, name = name, time = "2:00"),
              ),
            ),
          currentChapterNumber = 1,
          playing = false,
          onChapterClick = {},
          onDismiss = {},
        )
      }
    }
  }
}

private const val LONG_NAME =
  "In Which Our Heroes Leave the Harbour at Night, Lose Their Map in the Storm, " +
    "and Travel North Through the Old Forest to Meet the Clockmaker at Last"

private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
  val results = mutableListOf<TextLayoutResult>()
  val _ = fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!(results)
  return results.single()
}
