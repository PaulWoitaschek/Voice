package voice.features.playbackScreen.view

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import voice.core.data.BookId
import voice.core.strings.R
import voice.core.ui.VoiceTheme
import voice.features.playbackScreen.BookPlayViewState
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1400dp-h1400dp")
class BookPlayViewTest {

  @get:Rule
  val compose = createComposeRule()

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun `transport and every control that's shown stay inside the window`() {
    var window by mutableStateOf(DpSize(412.dp, 915.dp))
    var fontScale by mutableFloatStateOf(1F)
    // the seek bar wave never goes idle while playing
    compose.mainClock.autoAdvance = false
    compose.setContent {
      CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
        VoiceTheme {
          Box(Modifier.requiredSize(window).testTag(WINDOW)) {
            BookPlayView(
              viewState = viewState,
              bookId = BookId("book"),
              onPlayClick = {},
              onRewindClick = {},
              onFastForwardClick = {},
              onSeek = {},
              onSleepTimerClick = {},
              onBookmarkClick = {},
              onBookmarkLongClick = {},
              onSpeedChangeClick = {},
              onSkipSilenceClick = {},
              onVolumeBoostClick = {},
              onSkipToNext = {},
              onSkipToPrevious = {},
              onCloseClick = {},
              onBookActionsClick = {},
              onCurrentChapterClick = {},
              onJumpBack = {},
              onJumpBackExpire = {},
            )
          }
        }
      }
    }

    listOf(1F, 2F).forEach { scale ->
      listOf(
        DpSize(412.dp, 915.dp),
        DpSize(360.dp, 640.dp),
        // split screen panes on a phone
        DpSize(384.dp, 400.dp),
        DpSize(384.dp, 300.dp),
        DpSize(384.dp, 240.dp),
        DpSize(915.dp, 412.dp),
        DpSize(640.dp, 360.dp),
        DpSize(700.dp, 240.dp),
        DpSize(1280.dp, 800.dp),
        DpSize(800.dp, 1280.dp),
        DpSize(1920.dp, 1200.dp),
      ).forEach { size ->
        window = size
        fontScale = scale
        compose.mainClock.advanceTimeBy(2_000)

        val windowBounds = compose.onNodeWithTag(WINDOW).getBoundsInRoot()
        val shown = compose.onAllNodes(hasClickAction().and(isPlaced)).fetchSemanticsNodes()
        listOf(R.string.playback_action_rewind, R.string.playback_action_pause, R.string.playback_action_fast_forward)
          .forEach { transport ->
            val id = compose.onNodeWithContentDescription(context.getString(transport)).fetchSemanticsNode().id
            assertTrue(shown.any { it.id == id }, "$size at ${scale}x")
          }
        shown.forEach { node ->
          // unclipped, so a control scrolled out of view counts as outside
          val coordinates = node.layoutInfo.coordinates
          val bounds = with(compose.density) {
            Rect(coordinates.positionInRoot(), coordinates.size.toSize())
              .let { DpRect(it.left.toDp(), it.top.toDp(), it.right.toDp(), it.bottom.toDp()) }
          }
          val inside = bounds.left >= windowBounds.left && bounds.right <= windowBounds.right &&
            bounds.top >= windowBounds.top && bounds.bottom <= windowBounds.bottom
          assertTrue(inside, "$size at ${scale}x: ${node.config}")
        }
      }
    }
  }

  private val isPlaced = SemanticsMatcher("is placed") { it.layoutInfo.isPlaced }

  private val viewState = BookPlayViewState(
    chapterName = "The Signal",
    showPreviousNextButtons = true,
    cover = null,
    duration = 42.minutes,
    playedTime = 17.minutes,
    playing = true,
    skipSilence = false,
    sleepTimerState = BookPlayViewState.SleepTimerViewState.Disabled,
    title = "Echoes of Tomorrow",
    author = "Daniel Hartwell",
    playbackSpeed = 1F,
    volumeBoostActive = false,
    chapterNumber = 5,
    chapterCount = 12,
    bookPlayedTime = 4.hours + 12.minutes,
    bookDuration = 9.hours + 30.minutes,
    chapterSegments = List(12) { 1F / 12 },
    skipSeconds = 20,
    jumpBack = null,
  )
}

private const val WINDOW = "window"
