package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.ui.VoiceTheme
import voice.features.playbackScreen.BookPlayViewState
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
class BookPlayContentTest {

  @get:Rule
  val compose = createComposeRule()

  @Before
  fun setUp() {
    // Marquee on a long title never goes idle; pump a fixed clock instead.
    compose.mainClock.autoAdvance = false
  }

  @Test
  fun `short windows keep rewind, play and forward fully visible and each clickable once`() {
    val player = launch()
    listOf(false, true).forEach { landscape ->
      listOf(360.dp to 240.dp, 640.dp to 240.dp).forEach { (width, height) ->
        player.update(width = width, height = height, landscape = landscape)
        assertTransportInside(width, height, playing = true)
        player.resetClicks()
        clickTransport(playing = true)
        assertEquals(expected = 1, actual = player.clicks.rewind)
        assertEquals(expected = 1, actual = player.clicks.play)
        assertEquals(expected = 1, actual = player.clicks.forward)
      }
    }
  }

  @Test
  fun `shrinking the window and restoring it keeps a single visible transport row`() {
    val player = launch(height = 1200.dp)
    listOf(false, true).forEach { landscape ->
      val width = if (landscape) 640.dp else 360.dp
      player.update(width = width, height = 1200.dp, landscape = landscape)
      assertTransportInside(width, player.height, playing = true)

      player.update(height = 240.dp)
      assertTransportInside(width, player.height, playing = true)
      assertSingleTransport(playing = true)

      player.update(height = 180.dp)
      assertTransportInside(width, player.height, playing = true)
      assertSingleTransport(playing = true)

      player.update(height = 1200.dp)
      assertTransportInside(width, player.height, playing = true)
      assertSingleTransport(playing = true)
      assertRestoredArrangement(width, player.height, landscape)
      player.resetClicks()
      clickTransport(playing = true)
      assertEquals(expected = 1, actual = player.clicks.rewind)
      assertEquals(expected = 1, actual = player.clicks.play)
      assertEquals(expected = 1, actual = player.clicks.forward)
    }
  }

  @Test
  fun `secondary content can scroll without moving transport or letting the cover cover it`() {
    val player = launch()
    val scenarios = listOf(
      Scenario(),
      Scenario(chapters = 1, author = null, playing = false),
      Scenario(
        title = "The Extraordinary Voyage of the Last Lightkeeper Along the Winter Coast",
        author = "A Very Long Author Name That Should Not Push Transport Off Screen",
        fontScale = 1.5F,
      ),
      Scenario(chapters = 1, author = null, playing = false, fontScale = 2F),
      Scenario(chapters = 24, fontScale = 1.5F),
    )
    scenarios.forEach { scenario ->
      listOf(false to (360.dp to 240.dp), true to (640.dp to 240.dp)).forEach { (landscape, size) ->
        val (width, height) = size
        player.update(width = width, height = height, landscape = landscape, scenario = scenario)
        assertTransportInside(width, height, scenario.playing)
        val transportBefore = transportBounds(scenario.playing)
        val titleBefore = compose.onNodeWithText(scenario.title).boundsInPlayer().top
        swipeSecondaryContent()
        settle()
        assertEquals(expected = transportBefore, actual = transportBounds(scenario.playing))
        val titleAfter = compose.onNodeWithText(scenario.title).boundsInPlayer().top
        assertTrue(titleAfter < titleBefore)
        assertCoverDoesNotCoverTransport(scenario.playing)
      }
    }
  }

  @Test
  fun `tall portrait and landscape keep the transport in the current arrangement`() {
    val player = launch(width = 400.dp, height = 1200.dp, landscape = false)
    assertTransportInside(400.dp, 1200.dp, playing = true)
    assertClose(playBounds(playing = true).height, 104.dp)
    val cover = compose.onNodeWithContentDescription("Cover").boundsInPlayer()
    assertTrue(cover.height >= 180.dp)
    assertTrue(cover.bottom <= playBounds(playing = true).top)
    val toolbar = compose.onNodeWithContentDescription("Sleep timer").boundsInPlayer()
    assertTrue(toolbar.top >= playBounds(playing = true).bottom)

    player.update(width = 1200.dp, height = 700.dp, landscape = true)
    assertTransportInside(1200.dp, 700.dp, playing = true)
    assertClose(playBounds(playing = true).height, 80.dp)
    val play = playBounds(playing = true)
    val usableCenter = (TopInset + (700.dp - BottomInset)) / 2
    val playCenter = play.top + play.height / 2
    assertTrue(abs(playCenter.value - usableCenter.value) < 140F)
    assertInsideUsable(1200.dp, 700.dp, compose.onNodeWithContentDescription("Close").boundsInPlayer())
  }

  @Test
  fun `right to left layouts keep rewind, play and forward in left to right order`() {
    val player = launch(layoutDirection = LayoutDirection.Rtl)
    listOf(
      Triple(360.dp, 240.dp, false),
      Triple(640.dp, 240.dp, true),
      Triple(400.dp, 1200.dp, false),
    ).forEach { (width, height, landscape) ->
      player.update(width = width, height = height, landscape = landscape)
      assertTransportInside(width, height, playing = true)
      val rewind = compose.onNodeWithContentDescription("Rewind").boundsInPlayer()
      val play = playBounds(playing = true)
      val forward = compose.onNodeWithContentDescription("Fast forward").boundsInPlayer()
      assertTrue(rewind.left < play.left)
      assertTrue(play.left < forward.left)
      assertTrue(rewind.right <= play.left)
      assertTrue(play.right <= forward.left)
    }
  }

  private fun launch(
    width: Dp = 360.dp,
    height: Dp = 240.dp,
    landscape: Boolean = false,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
  ): Player {
    val player = Player(width, height, landscape, layoutDirection)
    player.bind { settle() }
    compose.setContent {
      val density = LocalDensity.current.density
      VoiceTheme {
        CompositionLocalProvider(
          LocalLayoutDirection provides player.layoutDirection,
          LocalDensity provides Density(density, player.scenario.fontScale),
        ) {
          Box(Modifier.requiredSize(player.width, player.height).testTag(PLAYER_TAG)) {
            val scenario = player.scenario
            BookPlayContent(
              contentPadding = PaddingValues(top = TopInset, bottom = BottomInset),
              viewState = scenario.viewState(),
              bookId = BookId("test"),
              clock = { 0F },
              useLandscapeLayout = player.landscape,
              onPlayClick = { player.clicks.play++ },
              onRewindClick = { player.clicks.rewind++ },
              onFastForwardClick = { player.clicks.forward++ },
              onSeek = {},
              onSkipToNext = {},
              onSkipToPrevious = {},
              onCurrentChapterClick = {},
              onSleepTimerClick = {},
              onSpeedChangeClick = {},
              onBookmarkClick = {},
              onBookmarkLongClick = {},
              onSkipSilenceClick = {},
              onVolumeBoostClick = {},
              onCloseClick = {},
              onBookActionsClick = {},
              onJumpBack = {},
              onJumpBackExpire = {},
            )
          }
        }
      }
    }
    settle()
    return player
  }

  private fun settle() {
    // autoAdvance is off so a long title's marquee cannot spin forever.
    compose.waitForIdle()
    compose.mainClock.advanceTimeBy(1_600)
    compose.waitForIdle()
  }

  private fun assertTransportInside(
    width: Dp,
    height: Dp,
    playing: Boolean,
  ) {
    val nodes = transportNodes(playing)
    nodes.rewind.assertCountEquals(1)
    nodes.play.assertCountEquals(1)
    nodes.forward.assertCountEquals(1)
    listOf(nodes.rewind[0], nodes.play[0], nodes.forward[0]).forEach { node ->
      node.assertHasClickAction()
      assertInsideUsable(width, height, node.boundsInPlayer())
      assertTrue(node.boundsInPlayer().height >= 48.dp)
    }
  }

  private fun assertInsideUsable(
    width: Dp,
    height: Dp,
    bounds: DpRect,
  ) {
    assertTrue(bounds.top >= TopInset - 1.dp)
    assertTrue(bounds.bottom <= height - BottomInset + 1.dp)
    assertTrue(bounds.left >= 0.dp)
    assertTrue(bounds.right <= width + 1.dp)
    assertTrue(bounds.width > 0.dp)
    assertTrue(bounds.height > 0.dp)
  }

  private fun assertSingleTransport(playing: Boolean) {
    val nodes = transportNodes(playing)
    nodes.rewind.assertCountEquals(1)
    nodes.play.assertCountEquals(1)
    nodes.forward.assertCountEquals(1)
  }

  private fun clickTransport(playing: Boolean) {
    compose.onNodeWithContentDescription("Rewind").performClick()
    compose.onNodeWithContentDescription(playLabel(playing)).performClick()
    compose.onNodeWithContentDescription("Fast forward").performClick()
  }

  private fun assertCoverDoesNotCoverTransport(playing: Boolean) {
    val covers = compose.onAllNodesWithContentDescription("Cover")
    if (covers.fetchSemanticsNodes().isEmpty()) return
    val cover = covers[0].boundsInPlayer()
    val play = playBounds(playing)
    val clearOfTransport = cover.height <= 0.dp ||
      cover.width <= 0.dp ||
      cover.bottom <= play.top ||
      cover.top >= play.bottom ||
      cover.right <= play.left ||
      cover.left >= play.right
    assertTrue(clearOfTransport)
  }

  private fun swipeSecondaryContent() {
    val scrollables = compose.onAllNodes(hasScrollAction())
    val tallest = scrollables.fetchSemanticsNodes().indices.maxBy { index ->
      scrollables[index].boundsInPlayer().height
    }
    scrollables[tallest].performTouchInput { swipeUp() }
  }

  private fun assertRestoredArrangement(
    width: Dp,
    height: Dp,
    landscape: Boolean,
  ) {
    val play = playBounds(playing = true)
    if (landscape) {
      val usableCenter = (TopInset + (height - BottomInset)) / 2
      val playCenter = play.top + play.height / 2
      assertTrue(abs(playCenter.value - usableCenter.value) < 140F)
      assertInsideUsable(width, height, compose.onNodeWithContentDescription("Close").boundsInPlayer())
    } else {
      assertClose(play.height, 104.dp)
    }
  }

  private fun transportBounds(playing: Boolean): List<DpRect> {
    val nodes = transportNodes(playing)
    return listOf(
      nodes.rewind[0].boundsInPlayer(),
      nodes.play[0].boundsInPlayer(),
      nodes.forward[0].boundsInPlayer(),
    )
  }

  private fun playBounds(playing: Boolean) = compose.onNodeWithContentDescription(playLabel(playing)).boundsInPlayer()

  private fun transportNodes(playing: Boolean) = TransportNodes(
    rewind = compose.onAllNodesWithContentDescription("Rewind"),
    play = compose.onAllNodesWithContentDescription(playLabel(playing)),
    forward = compose.onAllNodesWithContentDescription("Fast forward"),
  )

  private fun playLabel(playing: Boolean) = if (playing) "Pause" else "Play"

  // The host window centers a viewport larger than itself, so root coordinates are shifted.
  private fun SemanticsNodeInteraction.boundsInPlayer(): DpRect {
    val bounds = getUnclippedBoundsInRoot()
    val origin = compose.onNodeWithTag(PLAYER_TAG).getUnclippedBoundsInRoot()
    return DpRect(
      left = bounds.left - origin.left,
      top = bounds.top - origin.top,
      right = bounds.right - origin.left,
      bottom = bounds.bottom - origin.top,
    )
  }

  private fun assertClose(
    actual: Dp,
    expected: Dp,
  ) {
    assertTrue(abs(actual.value - expected.value) <= 1.5F)
  }
}

private class Player(
  width: Dp,
  height: Dp,
  landscape: Boolean,
  layoutDirection: LayoutDirection,
) {
  var width by mutableStateOf(width)
  var height by mutableStateOf(height)
  var landscape by mutableStateOf(landscape)
  var layoutDirection by mutableStateOf(layoutDirection)
  var scenario by mutableStateOf(Scenario())
  val clicks = TransportClicks()

  fun update(
    width: Dp = this.width,
    height: Dp = this.height,
    landscape: Boolean = this.landscape,
    scenario: Scenario = this.scenario,
  ) {
    this.width = width
    this.height = height
    this.landscape = landscape
    this.scenario = scenario
    settleForUpdate?.invoke()
  }

  private var settleForUpdate: (() -> Unit)? = null

  fun bind(settle: () -> Unit) {
    settleForUpdate = settle
  }

  fun resetClicks() {
    clicks.rewind = 0
    clicks.play = 0
    clicks.forward = 0
  }
}

private class TransportClicks {
  var rewind = 0
  var play = 0
  var forward = 0
}

private class TransportNodes(
  val rewind: SemanticsNodeInteractionCollection,
  val play: SemanticsNodeInteractionCollection,
  val forward: SemanticsNodeInteractionCollection,
)

private data class Scenario(
  val title: String = "Signal",
  val author: String? = "Ada Marin",
  val playing: Boolean = true,
  val chapters: Int = 8,
  val fontScale: Float = 1F,
) {
  fun viewState(): BookPlayViewState = BookPlayViewState(
    chapterName = if (chapters > 1) "The Signal" else null,
    showPreviousNextButtons = chapters > 1,
    title = title,
    author = author,
    sleepTimerState = BookPlayViewState.SleepTimerViewState.Disabled,
    playedTime = 17.minutes,
    duration = 42.minutes,
    playing = playing,
    cover = null,
    skipSilence = false,
    playbackSpeed = 1F,
    volumeBoostActive = false,
    chapterNumber = 1,
    chapterCount = chapters,
    bookPlayedTime = 17.minutes,
    bookDuration = 42.minutes,
    chapterSegments = List(chapters) { 1F / chapters },
    skipSeconds = 20,
    bookmarkPins = emptyList(),
    poppedPin = null,
    jumpBack = null,
  )
}

private val TopInset = 48.dp
private val BottomInset = 24.dp
private const val PLAYER_TAG = "player"
