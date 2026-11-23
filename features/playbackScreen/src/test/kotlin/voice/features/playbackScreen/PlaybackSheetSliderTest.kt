package voice.features.playbackScreen

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.children
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowDialog
import voice.core.playback.misc.Decibel
import voice.core.ui.VoiceTheme

@RunWith(AndroidJUnit4::class)
class PlaybackSheetSliderTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun `TalkBack on the slider of the speed sheet at 1_25x says Playback speed, 1_25x`() {
    var changedTo: Float? = null
    compose.setContent {
      VoiceTheme {
        SpeedSheet(
          dialogState = BookPlayDialogViewState.SpeedDialog(1.25F),
          onSpeedChange = { changedTo = it },
          onDismiss = {},
        )
      }
    }

    val slider = sliderInfo()
    assertEquals("Playback speed", slider.text.toString())
    assertEquals("1.25×", slider.stateDescription)

    scrollSliderForward()
    assertNotNull(changedTo)
  }

  @Test
  fun `TalkBack on the slider of the volume boost sheet at 3 dB says Volume boost, +3_0 dB`() {
    var changedTo: Decibel? = null
    compose.setContent {
      VoiceTheme {
        VolumeGainSheet(
          dialogState =
            BookPlayDialogViewState.VolumeGainDialog(
              gain = Decibel(3F),
              valueFormatted = VolumeGainFormatter().format(Decibel(3F)),
              maxGain = Decibel(9F),
            ),
          onGainChange = { changedTo = it },
          onDismiss = {},
        )
      }
    }

    val slider = sliderInfo()
    assertEquals("Volume boost", slider.text.toString())
    assertEquals("+3.0 dB", slider.stateDescription)

    scrollSliderForward()
    assertNotNull(changedTo)
  }

  private fun sliderInfo(): AccessibilityNodeInfoCompat {
    return provider().createAccessibilityNodeInfo(sliderId())!!
  }

  private fun scrollSliderForward() {
    provider().performAction(sliderId(), AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD, null)
    compose.waitForIdle()
  }

  private fun sliderId(): Int {
    return compose
      .onNode(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
        useUnmergedTree = true,
      )
      .fetchSemanticsNode()
      .id
  }

  // the sheet has its own window
  private fun provider() =
    composeView(ShadowDialog.getLatestDialog().window!!.decorView)!!.let { view ->
      ViewCompat.getAccessibilityDelegate(view)!!.getAccessibilityNodeProvider(view)!!
    }

  private fun composeView(view: View): View? {
    if (view is ViewRootForTest) return view
    return (view as? ViewGroup)?.children?.firstNotNullOfOrNull(::composeView)
  }
}
