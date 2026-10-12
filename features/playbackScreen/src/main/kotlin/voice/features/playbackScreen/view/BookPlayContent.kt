@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen.view

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import voice.core.data.BookId
import voice.core.ui.entrance
import voice.core.ui.rememberEntranceState
import voice.features.playbackScreen.BookPlayViewState
import kotlin.time.Duration

@Composable
internal fun BookPlayContent(
  contentPadding: PaddingValues,
  viewState: BookPlayViewState,
  bookId: BookId,
  clock: () -> Float,
  useLandscapeLayout: Boolean,
  onPlayClick: () -> Unit,
  onRewindClick: () -> Unit,
  onFastForwardClick: () -> Unit,
  onSeek: (Duration) -> Unit,
  onSkipToNext: () -> Unit,
  onSkipToPrevious: () -> Unit,
  onCurrentChapterClick: () -> Unit,
  onSleepTimerClick: () -> Unit,
  onSpeedChangeClick: () -> Unit,
  onBookmarkClick: () -> Unit,
  onBookmarkLongClick: () -> Unit,
  onSkipSilenceClick: () -> Unit,
  onVolumeBoostClick: () -> Unit,
  onCloseClick: () -> Unit,
  onBookActionsClick: () -> Unit,
  onJumpBack: (id: Long) -> Unit,
  onJumpBackExpire: (id: Long) -> Unit,
) {
  val entrance = rememberEntranceState()
  val cover: @Composable (Modifier) -> Unit = { modifier ->
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
      PlaybackCover(
        bookId = bookId,
        cover = viewState.cover,
        playing = viewState.playing,
        swipeEnabled = viewState.showPreviousNextButtons,
        onPlayPause = onPlayClick,
        onSwipeToNext = onSkipToNext,
        onSwipeToPrevious = onSkipToPrevious,
        modifier = Modifier.aspectRatio(1F),
      )
    }
  }
  val bookProgress: @Composable () -> Unit = {
    MediaControlsDirection {
      BookProgress(
        viewState = viewState,
        modifier = Modifier
          .fillMaxWidth()
          .entrance(entrance, 1),
      )
    }
  }
  // Title, progress, chapter and seek. The spacer below seek is the gap that sits above transport.
  val aboveTransport: @Composable (compact: Boolean) -> Unit = { compact ->
    TitleBlock(
      title = viewState.title,
      author = viewState.author,
      modifier = Modifier
        .fillMaxWidth()
        .entrance(entrance, 0),
    )
    if (!compact) {
      Spacer(Modifier.height(12.dp))
      bookProgress()
    }
    MediaControlsDirection {
      if (viewState.showPreviousNextButtons) {
        Spacer(Modifier.height(if (compact) 12.dp else 16.dp))
        ChapterSwitcher(
          chapterName = viewState.chapterName,
          chapterNumber = viewState.chapterNumber,
          chapterCount = viewState.chapterCount,
          onSkipToPrevious = onSkipToPrevious,
          onSkipToNext = onSkipToNext,
          onChapterClick = onCurrentChapterClick,
          modifier = Modifier.entrance(entrance, 2),
        )
      }
      Spacer(Modifier.height(12.dp))
      SeekSection(
        playedTime = viewState.playedTime,
        duration = viewState.duration,
        playing = viewState.playing,
        clock = clock,
        onSeek = onSeek,
        jumpBack = viewState.jumpBack,
        unavailable = viewState.unavailable,
        onJumpBack = onJumpBack,
        onJumpBackExpire = onJumpBackExpire,
        modifier = Modifier
          .fillMaxWidth()
          .entrance(entrance, 3),
      )
      Spacer(Modifier.height(8.dp))
    }
  }
  val transport: @Composable (playButtonSize: Dp) -> Unit = { playButtonSize ->
    MediaControlsDirection {
      TransportControls(
        bookId = bookId,
        playing = viewState.playing,
        skipSeconds = viewState.skipSeconds,
        onRewindClick = onRewindClick,
        onPlayClick = onPlayClick,
        onFastForwardClick = onFastForwardClick,
        playButtonSize = playButtonSize,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
  val belowTransport: @Composable (compact: Boolean) -> Unit = { compact ->
    Spacer(Modifier.height(if (compact) 12.dp else 20.dp))
    ActionToolbar(
      sleepTimerState = viewState.sleepTimerState,
      playbackSpeed = viewState.playbackSpeed,
      skipSilence = viewState.skipSilence,
      volumeBoostActive = viewState.volumeBoostActive,
      onSleepTimerClick = onSleepTimerClick,
      onSpeedClick = onSpeedChangeClick,
      onBookmarkClick = onBookmarkClick,
      onBookmarkLongClick = onBookmarkLongClick,
      onSkipSilenceClick = onSkipSilenceClick,
      onVolumeBoostClick = onVolumeBoostClick,
      // with labels the toolbar may get wider than the padded column on narrow phones
      modifier = Modifier
        .wrapContentWidth(unbounded = true)
        .entrance(entrance, 4),
    )
  }
  // Switching branches composes transport once. A couple of dp of slack keeps a rounding
  // difference between the two measurements from flipping the branch every frame.
  var constrained by remember(useLandscapeLayout) { mutableStateOf(false) }

  if (useLandscapeLayout) {
    if (constrained) {
      ConstrainedLandscapeLayout(
        contentPadding = contentPadding,
        onFitsNormal = { constrained = false },
        coverColumn = {
          CoverColumn(
            onCloseClick = onCloseClick,
            onBookActionsClick = onBookActionsClick,
            cover = cover,
            bookProgress = bookProgress,
          )
        },
        secondary = {
          aboveTransport(true)
          belowTransport(true)
        },
        transport = { transport(COMPACT_PLAY_BUTTON) },
      )
    } else {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(contentPadding)
          .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CoverColumn(
          onCloseClick = onCloseClick,
          onBookActionsClick = onBookActionsClick,
          cover = cover,
          bookProgress = bookProgress,
        )
        Spacer(Modifier.width(32.dp))
        LandscapeControlsColumn(
          onOverflow = { constrained = true },
          content = {
            aboveTransport(true)
            transport(COMPACT_PLAY_BUTTON)
            belowTransport(true)
          },
        )
      }
    }
  } else if (constrained) {
    ConstrainedPortraitLayout(
      contentPadding = contentPadding,
      cover = cover,
      secondary = {
        aboveTransport(false)
        belowTransport(false)
      },
      transport = { transport(COMPACT_PLAY_BUTTON) },
      onFitsNormal = { constrained = false },
    )
  } else {
    PortraitLayout(
      contentPadding = contentPadding,
      onOverflow = { constrained = true },
      cover = {
        cover(
          Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        )
      },
      controls = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          aboveTransport(false)
          transport(PORTRAIT_PLAY_BUTTON)
          belowTransport(false)
        }
      },
    )
  }
}

/**
 * The cover takes the height the controls leave, down to [MIN_PORTRAIT_COVER_HEIGHT]. Shorter than
 * that, this arrangement would push transport below the window, so [onOverflow] switches to the
 * constrained layout. Scrolling spans the whole screen so the content padding doesn't clip the
 * cover's glow.
 */
@Composable
private fun PortraitLayout(
  contentPadding: PaddingValues,
  onOverflow: () -> Unit,
  cover: @Composable () -> Unit,
  controls: @Composable () -> Unit,
) {
  val layoutDirection = LocalLayoutDirection.current
  val scrollState = rememberScrollState()
  val scrollable = scrollState.maxValue > 0
  var controlsHeight by remember { mutableIntStateOf(0) }
  val density = LocalDensity.current
  BoxWithConstraints(Modifier.fillMaxSize()) {
    val viewportHeight = constraints.maxHeight
    val top = with(density) { contentPadding.calculateTopPadding().roundToPx() }
    val bottom = with(density) { contentPadding.calculateBottomPadding().roundToPx() }
    val overflows = controlsHeight > 0 &&
      controlsHeight + with(density) { MIN_PORTRAIT_COVER_HEIGHT.roundToPx() } > viewportHeight - top - bottom
    val onOverflowUpdated by rememberUpdatedState(onOverflow)
    LaunchedEffect(overflows) {
      if (overflows) onOverflowUpdated()
    }
    Layout(
      contents = listOf(
        cover,
        {
          Box(Modifier.onSizeChanged { controlsHeight = it.height }) {
            controls()
          }
        },
      ),
      modifier = Modifier
        .then(if (scrollable) Modifier.fadeOutUnderTopBar(scrollState, contentPadding.calculateTopPadding()) else Modifier)
        // without anything to scroll, dragging shouldn't stretch the screen
        .verticalScroll(scrollState, enabled = scrollable)
        .padding(
          start = contentPadding.calculateStartPadding(layoutDirection) + 24.dp,
          end = contentPadding.calculateEndPadding(layoutDirection) + 24.dp,
        ),
    ) { (coverMeasurables, controlsMeasurables), constraints ->
      val width = constraints.maxWidth
      val controlsPlaceables = controlsMeasurables.map { it.measure(Constraints.fixedWidth(width)) }
      val measuredControlsHeight = controlsPlaceables.sumOf { it.height }
      val coverHeight = (viewportHeight - top - bottom - measuredControlsHeight)
        .coerceAtLeast(MIN_PORTRAIT_COVER_HEIGHT.roundToPx())
      val coverPlaceables = coverMeasurables.map { it.measure(Constraints.fixed(width, coverHeight)) }
      layout(width, top + coverHeight + measuredControlsHeight + bottom) {
        coverPlaceables.forEach { it.place(0, top) }
        var y = top + coverHeight
        controlsPlaceables.forEach {
          it.place(0, y)
          y += it.height
        }
      }
    }
  }
}

/**
 * Keeps the compact transport row on screen. The cover gets only the height left after that row
 * and the other controls, and can disappear so it never spends the transport's space. Whatever
 * doesn't fit scrolls above the row.
 */
@Composable
private fun ConstrainedPortraitLayout(
  contentPadding: PaddingValues,
  cover: @Composable (Modifier) -> Unit,
  secondary: @Composable () -> Unit,
  transport: @Composable () -> Unit,
  onFitsNormal: () -> Unit,
) {
  val layoutDirection = LocalLayoutDirection.current
  var secondaryHeight by remember { mutableIntStateOf(0) }
  val scrollState = rememberScrollState()
  val density = LocalDensity.current
  BoxWithConstraints(Modifier.fillMaxSize()) {
    val top = contentPadding.calculateTopPadding()
    val bottom = contentPadding.calculateBottomPadding()
    val available = with(density) { constraints.maxHeight - top.roundToPx() - bottom.roundToPx() }
    val fitsNormal = secondaryHeight > 0 &&
      with(density) {
        secondaryHeight + PORTRAIT_PLAY_BUTTON.roundToPx() + MIN_PORTRAIT_COVER_HEIGHT.roundToPx() +
          CONSTRAINT_RELEASE_SLACK.roundToPx() <= available
      }
    val onFitsNormalUpdated by rememberUpdatedState(onFitsNormal)
    LaunchedEffect(fitsNormal) {
      if (fitsNormal) onFitsNormalUpdated()
    }
    Column(
      Modifier
        .fillMaxSize()
        .padding(
          start = contentPadding.calculateStartPadding(layoutDirection) + 24.dp,
          end = contentPadding.calculateEndPadding(layoutDirection) + 24.dp,
          top = top,
          bottom = bottom,
        ),
    ) {
      BoxWithConstraints(
        Modifier
          .weight(1F)
          .fillMaxWidth(),
      ) {
        val viewport = constraints.maxHeight
        Layout(
          contents = listOf(
            { cover(Modifier.fillMaxSize()) },
            {
              Column(
                modifier = Modifier
                  .onSizeChanged { secondaryHeight = it.height }
                  .fillMaxWidth()
                  .padding(top = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                secondary()
              }
            },
          ),
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        ) { (coverMeasurables, secondaryMeasurables), constraints ->
          val width = constraints.maxWidth
          val secondaryPlaceables = secondaryMeasurables.map {
            it.measure(Constraints(maxWidth = width))
          }
          val measuredSecondary = secondaryPlaceables.sumOf { it.height }
          val coverHeight = (viewport - measuredSecondary).coerceAtLeast(0)
          val coverPlaceables = coverMeasurables.map {
            it.measure(Constraints.fixed(width, coverHeight))
          }
          layout(width, coverHeight + measuredSecondary) {
            coverPlaceables.forEach { it.place(0, 0) }
            var y = coverHeight
            secondaryPlaceables.forEach {
              it.place(0, y)
              y += it.height
            }
          }
        }
      }
      transport()
    }
  }
}

/** The landscape controls column, centered, scrolling only when it is taller than the window. */
@Composable
private fun RowScope.LandscapeControlsColumn(
  onOverflow: () -> Unit,
  content: @Composable () -> Unit,
) {
  var contentHeight by remember { mutableIntStateOf(0) }
  val density = LocalDensity.current
  BoxWithConstraints(
    modifier = Modifier
      .weight(1.3F)
      .fillMaxHeight(),
  ) {
    val overflows = contentHeight > 0 &&
      with(density) { contentHeight + LANDSCAPE_CONTROLS_VERTICAL_PADDING.roundToPx() * 2 > constraints.maxHeight }
    val onOverflowUpdated by rememberUpdatedState(onOverflow)
    LaunchedEffect(overflows) {
      if (overflows) onOverflowUpdated()
    }
    Column(
      modifier = Modifier
        .verticalScroll(rememberScrollState())
        .heightIn(min = maxHeight)
        .padding(vertical = LANDSCAPE_CONTROLS_VERTICAL_PADDING),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier
          .onSizeChanged { contentHeight = it.height }
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        content()
      }
    }
  }
}

/**
 * Reserves the compact transport row across the bottom, then gives the height that is left to the
 * cover column and the scrolling title, seek and actions. A narrow window cannot fit that row
 * inside the controls column alone.
 */
@Composable
private fun ConstrainedLandscapeLayout(
  contentPadding: PaddingValues,
  onFitsNormal: () -> Unit,
  coverColumn: @Composable RowScope.() -> Unit,
  secondary: @Composable () -> Unit,
  transport: @Composable () -> Unit,
) {
  var secondaryHeight by remember { mutableIntStateOf(0) }
  var transportHeight by remember { mutableIntStateOf(0) }
  val density = LocalDensity.current
  BoxWithConstraints(
    Modifier
      .fillMaxSize()
      .padding(contentPadding),
  ) {
    val fitsNormal = secondaryHeight > 0 && transportHeight > 0 &&
      with(density) {
        val padding = LANDSCAPE_CONTROLS_VERTICAL_PADDING.roundToPx() * 2
        secondaryHeight + transportHeight + padding + CONSTRAINT_RELEASE_SLACK.roundToPx() <= constraints.maxHeight
      }
    val onFitsNormalUpdated by rememberUpdatedState(onFitsNormal)
    LaunchedEffect(fitsNormal) {
      if (fitsNormal) onFitsNormalUpdated()
    }
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp)
        .padding(vertical = LANDSCAPE_CONTROLS_VERTICAL_PADDING),
    ) {
      Row(
        modifier = Modifier
          .weight(1F)
          .fillMaxWidth(),
      ) {
        coverColumn()
        Spacer(Modifier.width(32.dp))
        Box(
          modifier = Modifier
            .weight(1.3F)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        ) {
          Column(
            modifier = Modifier
              .onSizeChanged { secondaryHeight = it.height }
              .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            secondary()
          }
        }
      }
      Box(Modifier.onSizeChanged { transportHeight = it.height }) {
        transport()
      }
    }
  }
}

@Composable
private fun RowScope.CoverColumn(
  onCloseClick: () -> Unit,
  onBookActionsClick: () -> Unit,
  cover: @Composable (Modifier) -> Unit,
  bookProgress: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier
      .weight(1F)
      .fillMaxHeight()
      .padding(vertical = 8.dp),
  ) {
    Row(Modifier.fillMaxWidth()) {
      CloseButton(onClick = onCloseClick)
      Spacer(Modifier.weight(1F))
      BookActionsButton(onClick = onBookActionsClick)
    }
    cover(
      Modifier
        .weight(1F)
        .fillMaxWidth()
        .padding(vertical = 8.dp),
    )
    bookProgress()
  }
}

/** Fades out content scrolled under the transparent top bar, so it doesn't clash with its buttons. */
private fun Modifier.fadeOutUnderTopBar(
  scrollState: ScrollState,
  topBarBottom: Dp,
): Modifier = this
  .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
  .drawWithContent {
    drawContent()
    val fade = 24.dp.toPx()
    // gradually, so the cover's glow doesn't get cut off with the first scrolled pixel
    val hidden = (scrollState.value / fade).coerceIn(0F, 1F)
    drawRect(
      brush = Brush.verticalGradient(
        0F to Color.Black.copy(alpha = 1F - hidden),
        1F to Color.Black,
        // fully hidden behind the top bar's buttons, which end 8dp above the bar's bottom
        startY = topBarBottom.toPx() - 8.dp.toPx(),
        endY = topBarBottom.toPx() - 8.dp.toPx() + fade,
      ),
      blendMode = BlendMode.DstIn,
    )
  }

private val MIN_PORTRAIT_COVER_HEIGHT = 180.dp
private val PORTRAIT_PLAY_BUTTON = 104.dp
private val COMPACT_PLAY_BUTTON = 80.dp
private val LANDSCAPE_CONTROLS_VERTICAL_PADDING = 8.dp

/** Keeps a one-pixel measurement difference from flipping between the two layouts. */
private val CONSTRAINT_RELEASE_SLACK = 2.dp

@Composable
private fun TitleBlock(
  title: String,
  author: String?,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    Text(
      modifier = Modifier.basicMarquee(),
      text = title,
      style = MaterialTheme.typography.headlineSmallEmphasized,
      maxLines = 1,
    )
    if (author != null) {
      Text(
        modifier = Modifier.basicMarquee(),
        text = author,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}

/**
 * Media controls aren't mirrored in right to left layouts: like on a tape, playback runs from left
 * to right, so the timelines, chapter controls and transport controls stay in that order.
 */
@Composable
private fun MediaControlsDirection(content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
}
