package voice.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.compose.AsyncImage
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LetterCoverImageTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun `a renamed book without a cover shows the color of its new title`() {
    var title by mutableStateOf("Dune")
    compose.setContent {
      VoiceTheme {
        AsyncImage(
          model = rememberCoverThumbnailRequest(null),
          error = rememberLetterCover(title),
          contentDescription = null,
          modifier = Modifier.size(48.dp),
        )
      }
    }
    assertEquals(expected = letterCover("Dune", dark = false)!!.background, actual = corner())

    title = "Emma"

    assertEquals(expected = letterCover("Emma", dark = false)!!.background, actual = corner())
  }

  private fun corner(): Color {
    compose.waitForIdle()
    return compose.onRoot().captureToImage().toPixelMap()[1, 1]
  }
}
