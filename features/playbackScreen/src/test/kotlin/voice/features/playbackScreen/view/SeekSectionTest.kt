package voice.features.playbackScreen.view

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.ui.VoiceTheme

@RunWith(AndroidJUnit4::class)
class SeekSectionTest {

  @get:Rule val compose = createComposeRule()

  private lateinit var view: View
  private var seekedTo: Duration? = null
  private var playedTime by mutableStateOf(Duration.ZERO)

  @Test
  fun `TalkBack on the seek bar at 2_10 of 9_56 says Position, 2_10 of 9_56`() {
    setContent(
      playedTime = 2.minutes + 10.seconds + 400.milliseconds,
      duration = 9.minutes + 56.seconds,
    )

    val seekBar = provider().createAccessibilityNodeInfo(seekBarId())!!
    assertEquals("Position", seekBar.contentDescription)
    assertEquals("2:10 of 9:56", seekBar.stateDescription)
  }

  @Test
  fun `swipe up on the seek bar of a 10-hour book with one chapter jumps 30 seconds`() {
    val playedTime = 1.hours
    setContent(playedTime = playedTime, duration = 10.hours)

    provider().performAction(seekBarId(), AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD, null)

    assertEquals(30, (seekedTo!! - playedTime).toDouble(DurationUnit.SECONDS).roundToInt())
  }

  @Test
  fun `TalkBack on the seek bar while the book plays at 5_56 of 9_56 says nothing new until the next minute`() {
    setContent(
      playedTime = 5.minutes + 56.seconds,
      duration = 9.minutes + 56.seconds,
      playing = true,
    )
    val atStart = seekBarState()

    playedTime = 5.minutes + 59.seconds
    compose.waitForIdle()
    assertEquals(atStart, seekBarState())
    assertEquals("5 minutes of 9:56", atStart.first)

    playedTime = 6.minutes + 400.milliseconds
    compose.waitForIdle()
    assertEquals("6 minutes of 9:56", seekBarState().first)
  }

  @Test
  fun `swipe down on the seek bar while the book plays at 0_45 jumps 30 seconds back`() {
    val playedTime = 45.seconds
    setContent(playedTime = playedTime, duration = 9.minutes + 56.seconds, playing = true)

    val seekBar = provider().createAccessibilityNodeInfo(seekBarId())!!
    assertTrue(
      seekBar.actionList.any { it.id == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD },
    )
    provider().performAction(seekBarId(), AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD, null)

    val jumped = (playedTime - seekedTo!!).toDouble(DurationUnit.SECONDS).roundToInt()
    assertTrue(jumped in 29..31)
  }

  @Test
  fun `TalkBack after a swipe up on the seek bar while the book plays at 5_10 says the exact time until the next minute`() {
    setContent(
      playedTime = 5.minutes + 10.seconds + 400.milliseconds,
      duration = 9.minutes + 56.seconds,
      playing = true,
    )

    provider().performAction(seekBarId(), AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD, null)
    compose.waitForIdle()
    assertEquals("5:40 of 9:56", seekBarState().first)

    playedTime = 5.minutes + 50.seconds
    compose.waitForIdle()
    assertEquals("5:40 of 9:56", seekBarState().first)

    playedTime = 6.minutes + 400.milliseconds
    compose.waitForIdle()
    assertEquals("6 minutes of 9:56", seekBarState().first)
  }

  private fun seekBarState(): Pair<CharSequence?, Float> {
    val info = provider().createAccessibilityNodeInfo(seekBarId())!!
    return info.stateDescription to info.rangeInfo!!.current
  }

  private fun setContent(
    playedTime: Duration,
    duration: Duration,
    playing: Boolean = false,
  ) {
    this.playedTime = playedTime
    compose.setContent {
      view = LocalView.current
      VoiceTheme {
        SeekSection(
          playedTime = this.playedTime,
          duration = duration,
          playing = playing,
          clock = { 0F },
          onSeek = {
            seekedTo = it
            this.playedTime = it
          },
          jumpBack = null,
          onJumpBack = {},
          onJumpBackExpire = {},
        )
      }
    }
  }

  private fun seekBarId(): Int {
    return compose
      .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
      .fetchSemanticsNode()
      .id
  }

  private fun provider(): AccessibilityNodeProviderCompat {
    return ViewCompat.getAccessibilityDelegate(view)!!.getAccessibilityNodeProvider(view)!!
  }
}
