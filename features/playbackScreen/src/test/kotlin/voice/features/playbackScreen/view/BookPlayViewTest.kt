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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import voice.core.data.BookId
import voice.core.strings.R
import voice.core.ui.VoiceTheme
import voice.features.playbackScreen.BookPlayViewState

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1400dp-h1400dp")
class BookPlayViewTest {

  @get:Rule val compose = createComposeRule()

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun `transport and every control that's shown stay inside the window`() {
    var window by mutableStateOf(DpSize(412.dp, 915.dp))
    var fontScale by mutableFloatStateOf(1F)
    var state by mutableStateOf(viewState)
    // the seek bar wave never goes idle while playing
    compose.mainClock.autoAdvance = false
    setPlayer({ window }, { fontScale }, { state })

    val shownWithoutCard = mutableMapOf<Pair<DpSize, Float>, Int>()
    listOf(viewState, viewState.copy(upNext = upNext)).forEach { shownState ->
      listOf(1F, 2F).forEach { scale ->
        listOf(
            DpSize(412.dp, 915.dp),
            DpSize(360.dp, 640.dp),
            DpSize(384.dp, 400.dp),
            DpSize(384.dp, 300.dp),
            DpSize(384.dp, 240.dp),
            DpSize(915.dp, 412.dp),
            DpSize(640.dp, 360.dp),
            DpSize(700.dp, 240.dp),
            DpSize(1280.dp, 800.dp),
            DpSize(800.dp, 1280.dp),
            DpSize(1920.dp, 1200.dp),
          )
          .forEach { size ->
            window = size
            fontScale = scale
            state = shownState
            compose.mainClock.advanceTimeBy(2_000)

            val windowBounds = compose.onNodeWithTag(WINDOW).getBoundsInRoot()
            val shown = compose.onAllNodes(hasClickAction().and(isPlaced)).fetchSemanticsNodes()
            val cardShown = shown.any { node ->
              node.config.getOrNull(SemanticsProperties.Text).orEmpty().any {
                it.text == upNext.title
              }
            }
            if (shownState.upNext == null) {
              shownWithoutCard[size to scale] = shown.size
            } else if (!cardShown) {
              assertEquals(shownWithoutCard[size to scale], shown.size, "$size at ${scale}x")
            }
            listOf(
                R.string.playback_action_rewind,
                R.string.playback_action_pause,
                R.string.playback_action_fast_forward,
              )
              .forEach { transport ->
                val id =
                  compose
                    .onNodeWithContentDescription(context.getString(transport))
                    .fetchSemanticsNode()
                    .id
                assertTrue(shown.any { it.id == id }, "$size at ${scale}x, ${shownState.upNext}")
              }
            shown.forEach { node ->
              // unclipped, so a control scrolled out of view counts as outside
              val coordinates = node.layoutInfo.coordinates
              val bounds =
                with(compose.density) {
                  Rect(coordinates.positionInRoot(), coordinates.size.toSize()).let {
                    DpRect(it.left.toDp(), it.top.toDp(), it.right.toDp(), it.bottom.toDp())
                  }
                }
              val inside =
                bounds.left >= windowBounds.left &&
                  bounds.right <= windowBounds.right &&
                  bounds.top >= windowBounds.top &&
                  bounds.bottom <= windowBounds.bottom
              assertTrue(inside, "$size at ${scale}x, ${shownState.upNext}: ${node.config}")
            }
          }
      }
    }
  }

  @Test
  fun `a phone shows the up next card, and a tap opens its book`() {
    var opened: BookId? = null
    compose.mainClock.autoAdvance = false
    setPlayer(
      window = { DpSize(412.dp, 915.dp) },
      fontScale = { 1F },
      state = { viewState.copy(upNext = upNext) },
      onUpNextClick = { opened = it },
    )
    compose.mainClock.advanceTimeBy(2_000)

    compose.onNodeWithText(upNext.title).assertIsDisplayed().performClick()

    assertEquals(upNext.id, opened)
  }

  @Test
  fun `a card that doesn't fit stays out while it comes and goes`() {
    var state by mutableStateOf(viewState)
    compose.mainClock.autoAdvance = false
    setPlayer({ DpSize(384.dp, 300.dp) }, { 2F }, { state })

    listOf(viewState.copy(upNext = upNext), viewState).forEach { next ->
      compose.mainClock.advanceTimeBy(2_000)
      state = next
      repeat(30) {
        compose.mainClock.advanceTimeByFrame()
        val card = compose.onAllNodes(hasText(upNext.title).and(isPlaced)).fetchSemanticsNodes()
        assertTrue(card.isEmpty(), "frame $it of $next")
      }
    }
  }

  private fun setPlayer(
    window: () -> DpSize,
    fontScale: () -> Float,
    state: () -> BookPlayViewState,
    onUpNextClick: (BookId) -> Unit = {},
  ) {
    compose.setContent {
      CompositionLocalProvider(
        LocalDensity provides Density(LocalDensity.current.density, fontScale()),
      ) {
        VoiceTheme {
          Box(Modifier.requiredSize(window()).testTag(WINDOW)) {
            BookPlayView(
              viewState = state(),
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
              onAuthorClick = {},
              onCurrentChapterClick = {},
              onJumpBack = {},
              onJumpBackExpire = {},
              onUpNextClick = onUpNextClick,
            )
          }
        }
      }
    }
  }

  private val isPlaced = SemanticsMatcher("is placed") { it.layoutInfo.isPlaced }

  private val viewState =
    BookPlayViewState(
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
      skipAmount = 20.seconds,
      jumpBack = null,
    )

  private val upNext =
    BookPlayViewState.UpNextViewState(
      id = BookId("next"),
      title = "Interference",
      series = "The Signal Saga 3",
      cover = null,
      unavailable = false,
    )
}

private const val WINDOW = "window"
