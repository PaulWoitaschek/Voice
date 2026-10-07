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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
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
  onJumpBack: () -> Unit,
  onJumpBackExpire: () -> Unit,
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
  val controls: @Composable (playButtonSize: Dp, compact: Boolean) -> Unit = { playButtonSize, compact ->
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
        onJumpBack = onJumpBack,
        onJumpBackExpire = onJumpBackExpire,
        modifier = Modifier
          .fillMaxWidth()
          .entrance(entrance, 3),
      )
      Spacer(Modifier.height(8.dp))
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

  if (useLandscapeLayout) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
        .padding(horizontal = 24.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier
          .weight(1F)
          .fillMaxHeight()
          .padding(vertical = 8.dp),
      ) {
        CloseButton(onClick = onCloseClick)
        cover(
          Modifier
            .weight(1F)
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        )
        bookProgress()
      }
      Spacer(Modifier.width(32.dp))
      BoxWithConstraints(
        modifier = Modifier
          .weight(1.3F)
          .fillMaxHeight(),
      ) {
        Column(
          modifier = Modifier
            .verticalScroll(rememberScrollState())
            .heightIn(min = maxHeight)
            .padding(vertical = 8.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          controls(80.dp, true)
        }
      }
    }
  } else {
    PortraitLayout(
      contentPadding = contentPadding,
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
          controls(104.dp, false)
        }
      },
    )
  }
}

/**
 * The cover takes the height the controls leave. On short screens or with large fonts it keeps a
 * minimum size and the content scrolls instead, so the controls never get cut off.
 * The scrolling spans the whole screen, so the content padding doesn't clip the cover's glow.
 */
@Composable
private fun PortraitLayout(
  contentPadding: PaddingValues,
  cover: @Composable () -> Unit,
  controls: @Composable () -> Unit,
) {
  val layoutDirection = LocalLayoutDirection.current
  val scrollState = rememberScrollState()
  val scrollable = scrollState.maxValue > 0
  BoxWithConstraints(Modifier.fillMaxSize()) {
    val viewportHeight = constraints.maxHeight
    Layout(
      contents = listOf(cover, controls),
      modifier = Modifier
        .then(if (scrollable) Modifier.fadeOutUnderTopBar(scrollState, contentPadding.calculateTopPadding()) else Modifier)
        // without anything to scroll, dragging shouldn't stretch the screen
        .verticalScroll(scrollState, enabled = scrollable)
        .padding(
          start = contentPadding.calculateStartPadding(layoutDirection) + 24.dp,
          end = contentPadding.calculateEndPadding(layoutDirection) + 24.dp,
        ),
    ) { (coverMeasurables, controlsMeasurables), constraints ->
      val top = contentPadding.calculateTopPadding().roundToPx()
      val bottom = contentPadding.calculateBottomPadding().roundToPx()
      val width = constraints.maxWidth
      val controlsPlaceables = controlsMeasurables.map { it.measure(Constraints.fixedWidth(width)) }
      val controlsHeight = controlsPlaceables.sumOf { it.height }
      val coverHeight = (viewportHeight - top - bottom - controlsHeight)
        .coerceAtLeast(MIN_PORTRAIT_COVER_HEIGHT.roundToPx())
      val coverPlaceables = coverMeasurables.map { it.measure(Constraints.fixed(width, coverHeight)) }
      layout(width, top + coverHeight + controlsHeight + bottom) {
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
