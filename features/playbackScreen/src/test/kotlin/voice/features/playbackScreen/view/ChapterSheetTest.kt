package voice.features.playbackScreen.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.ui.VoiceTheme
import voice.features.playbackScreen.BookPlayDialogViewState.SelectChapterDialog
import voice.features.playbackScreen.ChapterSheet

@RunWith(AndroidJUnit4::class)
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
}
