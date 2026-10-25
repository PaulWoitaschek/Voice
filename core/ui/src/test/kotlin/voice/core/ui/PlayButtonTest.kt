package voice.core.ui

import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayButtonTest {

  @get:Rule
  val compose = createComposeRule(
    effectContext = object : MotionDurationScale {
      override val scaleFactor = 0F
    },
  )

  @Test
  fun `with animations off the button doesn't turn on click`() {
    compose.mainClock.autoAdvance = false
    compose.setContent {
      VoiceTheme {
        PlayButton(playing = false, onClick = {})
      }
    }
    compose.mainClock.advanceTimeByFrame()
    val before = compose.onRoot().captureToImage().asAndroidBitmap()

    // a click through semantics has no ripple, which would change the image
    compose.onNode(hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)
    compose.waitForIdle()
    compose.mainClock.advanceTimeBy(1_000)

    assertTrue(before.sameAs(compose.onRoot().captureToImage().asAndroidBitmap()))
  }
}
