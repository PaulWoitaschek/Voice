package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import voice.core.ui.VoiceTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkipButtonTest {

  private val motionScale = object : MotionDurationScale {
    override var scaleFactor by mutableFloatStateOf(0F)
  }

  @get:Rule
  val compose = createComposeRule(effectContext = motionScale)

  @Test
  fun `pressing skip buttons with animations off keeps both controls in place`() {
    setContent()
    val rewindBounds = compose.onNodeWithTag("rewind").getBoundsInRoot()
    val forwardBounds = compose.onNodeWithTag("forward").getBoundsInRoot()

    listOf("rewind", "forward").forEach { tag ->
      compose.onNodeWithTag(tag).performTouchInput { down(center) }
      compose.mainClock.advanceTimeBy(500)

      assertEquals(rewindBounds, compose.onNodeWithTag("rewind").getBoundsInRoot())
      assertEquals(forwardBounds, compose.onNodeWithTag("forward").getBoundsInRoot())

      compose.onNodeWithTag(tag).performTouchInput { up() }
      compose.mainClock.advanceTimeBy(1_000)
    }
  }

  @Test
  fun `press animation remains enabled and responds to system setting changes`() {
    motionScale.scaleFactor = 1F
    setContent()
    val initialBounds = compose.onNodeWithTag("rewind").getBoundsInRoot()
    compose.onNodeWithTag("rewind").performTouchInput { down(center) }
    compose.mainClock.advanceTimeBy(500)

    val pressedBounds = compose.onNodeWithTag("rewind").getBoundsInRoot()
    assertTrue(pressedBounds.right - pressedBounds.left > initialBounds.right - initialBounds.left)

    compose.runOnIdle { motionScale.scaleFactor = 0F }
    compose.mainClock.advanceTimeByFrame()
    compose.waitForIdle()
    compose.mainClock.advanceTimeBy(500)

    assertEquals(initialBounds, compose.onNodeWithTag("rewind").getBoundsInRoot())
    compose.onNodeWithTag("rewind").performTouchInput { up() }
  }

  @Test
  fun `skipping with animations off leaves the buttons as they are`() {
    setContent()
    val before = compose.onRoot().captureToImage().asAndroidBitmap()

    // a click through semantics has no ripple, which would change the image
    compose.onNode(hasClickAction() and hasParent(hasTestTag("forward"))).performSemanticsAction(SemanticsActions.OnClick)
    repeat(3) {
      compose.mainClock.advanceTimeByFrame()
      assertTrue(before.sameAs(compose.onRoot().captureToImage().asAndroidBitmap()))
    }
  }

  private fun setContent() {
    compose.mainClock.autoAdvance = false
    compose.setContent {
      VoiceTheme {
        Row {
          SkipButton(forward = false, seconds = 10, onClick = {}, modifier = Modifier.height(72.dp).testTag("rewind"))
          SkipButton(forward = true, seconds = 10, onClick = {}, modifier = Modifier.height(72.dp).testTag("forward"))
        }
      }
    }
    compose.mainClock.advanceTimeByFrame()
  }
}
