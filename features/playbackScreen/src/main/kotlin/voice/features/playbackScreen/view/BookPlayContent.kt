@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen.view

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
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
import voice.core.ui.MaxContentWidth
import voice.core.ui.bookText
import voice.core.ui.entrance
import voice.core.ui.rememberEntranceState
import voice.features.playbackScreen.BookPlayViewState
import java.text.NumberFormat
import kotlin.time.Duration

@Composable
internal fun BookPlayContent(
  contentPadding: PaddingValues,
  viewState: BookPlayViewState,
  bookId: BookId,
  clock: () -> Float,
  sideBySide: Boolean,
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
  // with a single chapter, the seek bar already spans the whole book
  val showBookProgress = viewState.chapterCount > 1
  val percentFormat = remember { NumberFormat.getPercentInstance() }
  val title: @Composable () -> Unit = {
    TitleBlock(
      title = viewState.title,
      author = viewState.author,
      modifier = Modifier
        .fillMaxWidth()
        .entrance(entrance, 0),
    )
  }
  val control: @Composable (Control) -> Unit = { control ->
    when (control) {
      // side by side, these go under the cover, so phones in landscape fit the controls
      Control.Title -> if (!sideBySide) title()
      Control.BookProgress -> if (!sideBySide && showBookProgress) bookProgress()
      Control.Chapter -> if (viewState.showPreviousNextButtons) {
        MediaControlsDirection {
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
      }
      Control.Seek -> MediaControlsDirection {
        SeekSection(
          playedTime = viewState.playedTime,
          duration = viewState.duration,
          playing = viewState.playing,
          clock = clock,
          onSeek = onSeek,
          jumpBack = viewState.jumpBack,
          unavailable = viewState.unavailable,
          bookPercent = if (showBookProgress) null else percentFormat.format(viewState.bookProgress.toDouble()),
          onJumpBack = onJumpBack,
          onJumpBackExpire = onJumpBackExpire,
          modifier = Modifier
            .fillMaxWidth()
            .entrance(entrance, 3),
        )
      }
      // ControlsLayout picks the height, and the play button fills it. Reading it with BoxWithConstraints instead
      // would keep the shared play button from flying in when the player opens.
      Control.Transport -> MediaControlsDirection {
        TransportControls(
          bookId = bookId,
          playing = viewState.playing,
          skipSeconds = viewState.skipSeconds,
          onRewindClick = onRewindClick,
          onPlayClick = onPlayClick,
          onFastForwardClick = onFastForwardClick,
          modifier = Modifier.fillMaxWidth(),
        )
      }
      Control.Toolbar -> ActionToolbar(
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
  }

  if (sideBySide) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
        .padding(horizontal = HORIZONTAL_PADDING),
      horizontalArrangement = Arrangement.Center,
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
          // the chapter switcher is the first to go when the window gets short
          if (viewState.showPreviousNextButtons) {
            ChaptersButton(onClick = onCurrentChapterClick)
          }
          BookActionsButton(onClick = onBookActionsClick)
        }
        cover(
          Modifier
            .weight(1F)
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        )
        title()
        if (showBookProgress) {
          Spacer(Modifier.height(12.dp))
          bookProgress()
        }
      }
      Spacer(Modifier.width(32.dp))
      ControlsLayout(
        cover = null,
        control = control,
        modifier = Modifier
          .weight(1.3F, fill = false)
          .widthIn(max = MaxContentWidth)
          .fillMaxWidth()
          .fillMaxHeight(),
      )
    }
  } else {
    ControlsLayout(
      cover = {
        cover(
          Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        )
      },
      control = control,
      modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
        .wrapContentWidth()
        .widthIn(max = MaxContentWidth)
        .padding(horizontal = HORIZONTAL_PADDING),
    )
  }
}

/** The controls in the order they are stacked. */
private enum class Control(
  val gapAbove: Dp,
  val compactGapAbove: Dp,
) {
  Title(0.dp, 0.dp),
  BookProgress(12.dp, 12.dp),
  Chapter(16.dp, 12.dp),
  Seek(12.dp, 12.dp),
  Transport(8.dp, 8.dp),
  Toolbar(20.dp, 12.dp),
}

/** When the height runs out, the controls give way from the end of this list. */
private val CONTROL_PRIORITY = listOf(
  Control.Transport,
  Control.Title,
  Control.Seek,
  Control.Toolbar,
  Control.Chapter,
  Control.BookProgress,
)

/**
 * Stacks the controls. When the height runs out, whole controls are left out in [CONTROL_PRIORITY]
 * order, so transport stays on screen and nothing that's shown is cut off or scrolled away.
 *
 * With a [cover], the controls sit at the bottom and the cover takes the height above them. When
 * that's less than [MIN_COVER_HEIGHT], the controls turn compact first, and if that's not enough,
 * the cover gives way before any of the controls. Without a cover, the controls are centered, and
 * compact only when they don't fit otherwise.
 */
@Composable
private fun ControlsLayout(
  cover: (@Composable () -> Unit)?,
  control: @Composable (Control) -> Unit,
  modifier: Modifier = Modifier,
) {
  Layout(
    contents = listOf(cover ?: {}) + Control.entries.map { entry -> @Composable { control(entry) } },
    modifier = modifier,
  ) { slots, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val coverMeasurable = slots.first().firstOrNull()
    val measurables = Control.entries.zip(slots.drop(1))
      .mapNotNull { (control, measurables) -> measurables.firstOrNull()?.let { control to it } }
      .toMap()
    // transport is measured once its size is decided
    val placeables = measurables
      .filterKeys { it != Control.Transport }
      .mapValues { it.value.measure(Constraints.fixedWidth(width)) }

    fun playButtonSize(compact: Boolean): Int = (if (compact) COMPACT_PLAY_BUTTON else REGULAR_PLAY_BUTTON).roundToPx()
    fun verticalPadding(compact: Boolean): Int = (if (compact) 8.dp else 16.dp).roundToPx()
    fun gapAbove(
      control: Control,
      compact: Boolean,
    ): Int = (if (compact) control.compactGapAbove else control.gapAbove).roundToPx()
    fun heightOf(
      controls: List<Control>,
      compact: Boolean,
    ): Int = 2 * verticalPadding(compact) + controls.sumOf { control ->
      val controlHeight = placeables[control]?.height ?: playButtonSize(compact)
      controlHeight + if (control == controls.first()) 0 else gapAbove(control, compact)
    }

    val all = measurables.keys.sorted()
    val minCover = MIN_COVER_HEIGHT.roundToPx()
    val compact = heightOf(all, compact = false) + (if (coverMeasurable != null) minCover else 0) > height
    val showCover = coverMeasurable != null && heightOf(all, compact) + minCover <= height
    var shown = all
    if (!showCover) {
      shown = emptyList()
      for (control in CONTROL_PRIORITY.filter { it in measurables }) {
        val more = (shown + control).sorted()
        // transport stays, even when it doesn't fit
        if (shown.isNotEmpty() && heightOf(more, compact) > height) break
        shown = more
      }
    }
    val controlsHeight = heightOf(shown, compact)
    val coverPlaceable = if (showCover) {
      coverMeasurable.measure(Constraints.fixed(width, height - controlsHeight))
    } else {
      null
    }
    val playButtonSize = playButtonSize(compact)
    val transportPlaceable = measurables.getValue(Control.Transport)
      .measure(Constraints.fixed(width, playButtonSize))
    layout(width, height) {
      coverPlaceable?.place(0, 0)
      var y = verticalPadding(compact) + if (coverPlaceable != null) height - controlsHeight else (height - controlsHeight) / 2
      shown.forEach { control ->
        if (control != shown.first()) y += gapAbove(control, compact)
        val placeable = placeables[control] ?: transportPlaceable
        placeable.place(0, y)
        y += placeable.height
      }
    }
  }
}

private val HORIZONTAL_PADDING = 24.dp
private val REGULAR_PLAY_BUTTON = 104.dp
private val COMPACT_PLAY_BUTTON = 80.dp
private val MIN_COVER_HEIGHT = 120.dp

@Composable
private fun TitleBlock(
  title: String,
  author: String?,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    MarqueeText(
      text = title,
      style = MaterialTheme.typography.headlineSmallEmphasized.bookText(),
      bleed = HORIZONTAL_PADDING,
    )
    if (author != null) {
      MarqueeText(
        text = author,
        style = MaterialTheme.typography.titleMedium.bookText(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        bleed = HORIZONTAL_PADDING,
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
