package voice.playbackScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import voice.common.compose.ImmutableFile
import voice.common.compose.VoiceTheme
import voice.common.formatTime
import voice.common.recomposeHighlighter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun BookPlayView(
  viewState: BookPlayViewState,
  useLandscapeLayout: Boolean,
  onPlayClick: () -> Unit,
  onRewindClick: () -> Unit,
  onFastForwardClick: () -> Unit,
  onSeek: (Duration) -> Unit,
  onSleepTimerClick: () -> Unit,
  onBookmarkClick: () -> Unit,
  onBookmarkLongClick: () -> Unit,
  onSpeedChangeClick: () -> Unit,
  onSkipSilenceClick: () -> Unit,
  onVolumeBoostClick: () -> Unit,
  onSkipToNext: () -> Unit,
  onSkipToPrevious: () -> Unit,
  onCloseClick: () -> Unit,
  onCurrentChapterClick: () -> Unit,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
  Scaffold(
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState)
    },
    topBar = {
      BookPlayAppBar(
        viewState = viewState,
        onSleepTimerClick = onSleepTimerClick,
        onBookmarkClick = onBookmarkClick,
        onBookmarkLongClick = onBookmarkLongClick,
        onSpeedChangeClick = onSpeedChangeClick,
        onSkipSilenceClick = onSkipSilenceClick,
        onVolumeBoostClick = onVolumeBoostClick,
        onCloseClick = onCloseClick,
        useLandscapeLayout = useLandscapeLayout,
      )
    },
    content = {
      BookPlayContent(
        contentPadding = it,
        viewState = viewState,
        onPlayClick = onPlayClick,
        onRewindClick = onRewindClick,
        onFastForwardClick = onFastForwardClick,
        onSeek = onSeek,
        onSkipToNext = onSkipToNext,
        onSkipToPrevious = onSkipToPrevious,
        onCurrentChapterClick = onCurrentChapterClick,
        useLandscapeLayout = useLandscapeLayout,
      )
    },
  )
}

@Composable
private fun BookPlayContent(
  contentPadding: PaddingValues,
  viewState: BookPlayViewState,
  onPlayClick: () -> Unit,
  onRewindClick: () -> Unit,
  onFastForwardClick: () -> Unit,
  onSeek: (Duration) -> Unit,
  onSkipToNext: () -> Unit,
  onSkipToPrevious: () -> Unit,
  onCurrentChapterClick: () -> Unit,
  useLandscapeLayout: Boolean,
) {
  if (useLandscapeLayout) {
    Row(Modifier.padding(contentPadding)) {
      CoverRow(
        cover = viewState.cover,
        onPlayClick = onPlayClick,
        sleepTime = viewState.sleepTime,
        modifier = Modifier
          .fillMaxHeight()
          .weight(1F)
          .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
      )
      Column(
        modifier = Modifier
          .fillMaxHeight()
          .weight(1F),
        verticalArrangement = Arrangement.Center,
      ) {
        viewState.chapterName?.let { chapterName ->
          ChapterRow(
            chapterName = chapterName,
            nextPreviousVisible = viewState.showPreviousNextButtons,
            onSkipToNext = onSkipToNext,
            onSkipToPrevious = onSkipToPrevious,
            onCurrentChapterClick = onCurrentChapterClick,
          )
        }
        Spacer(modifier = Modifier.size(20.dp))
        SliderRow(viewState, onSeek = onSeek)
        Spacer(modifier = Modifier.size(16.dp))
        PlaybackRow(
          playing = viewState.playing,
          onPlayClick = onPlayClick,
          onRewindClick = onRewindClick,
          onFastForwardClick = onFastForwardClick,
        )
      }
    }
  } else {
    Column(Modifier.padding(contentPadding)) {
      CoverRow(
        onPlayClick = onPlayClick,
        cover = viewState.cover,
        sleepTime = viewState.sleepTime,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1F)
          .padding(start = 16.dp, end = 16.dp, top = 8.dp),
      )
      viewState.chapterName?.let { chapterName ->
        Spacer(modifier = Modifier.size(16.dp))
        ChapterRow(
          chapterName = chapterName,
          nextPreviousVisible = viewState.showPreviousNextButtons,
          onSkipToNext = onSkipToNext,
          onSkipToPrevious = onSkipToPrevious,
          onCurrentChapterClick = onCurrentChapterClick,
        )
      }
      Spacer(modifier = Modifier.size(20.dp))
      SliderRow(viewState, onSeek = onSeek)
      Spacer(modifier = Modifier.size(16.dp))
      PlaybackRow(
        playing = viewState.playing,
        onPlayClick = onPlayClick,
        onRewindClick = onRewindClick,
        onFastForwardClick = onFastForwardClick,
      )
      Spacer(modifier = Modifier.size(24.dp))
    }
  }
}

@Composable
private fun BookPlayAppBar(
  viewState: BookPlayViewState,
  onSleepTimerClick: () -> Unit,
  onBookmarkClick: () -> Unit,
  onBookmarkLongClick: () -> Unit,
  onSpeedChangeClick: () -> Unit,
  onSkipSilenceClick: () -> Unit,
  onVolumeBoostClick: () -> Unit,
  onCloseClick: () -> Unit,
  useLandscapeLayout: Boolean,
) {
  val appBarActions: @Composable RowScope.() -> Unit = {
    IconButton(onClick = onSleepTimerClick) {
      Icon(
        imageVector = if (viewState.sleepTime == ZERO) {
          Icons.Outlined.Alarm
        } else {
          Icons.Outlined.AlarmOff
        },
        contentDescription = stringResource(id = R.string.action_sleep),
      )
    }
    Box(
      modifier = Modifier
        .size(40.dp)
        .combinedClickable(
          onClick = onBookmarkClick,
          onLongClick = onBookmarkLongClick,
          indication = rememberRipple(bounded = false, radius = 20.dp),
          interactionSource = remember { MutableInteractionSource() },
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Outlined.CollectionsBookmark,
        contentDescription = stringResource(id = R.string.bookmark),
      )
    }
    IconButton(onClick = onSpeedChangeClick) {
      Icon(
        imageVector = Icons.Outlined.Speed,
        contentDescription = stringResource(id = R.string.playback_speed),
      )
    }
    OverflowMenu(
      skipSilence = viewState.skipSilence,
      onSkipSilenceClick = onSkipSilenceClick,
      onVolumeBoostClick = onVolumeBoostClick,
    )
  }
  if (useLandscapeLayout) {
    TopAppBar(
      navigationIcon = {
        CloseIcon(onCloseClick)
      },
      actions = appBarActions,
      title = {
        AppBarTitle(viewState)
      },
    )
  } else {
    LargeTopAppBar(
      navigationIcon = {
        CloseIcon(onCloseClick)
      },
      actions = appBarActions,
      title = {
        AppBarTitle(viewState)
      },
    )
  }
}

@Composable
private fun AppBarTitle(viewState: BookPlayViewState) {
  Text(text = viewState.title)
}

@Composable
private fun CloseIcon(onCloseClick: () -> Unit) {
  IconButton(onClick = onCloseClick) {
    Icon(
      imageVector = Icons.Outlined.Close,
      contentDescription = stringResource(id = R.string.close),
    )
  }
}

@Composable
private fun OverflowMenu(
  skipSilence: Boolean,
  onSkipSilenceClick: () -> Unit,
  onVolumeBoostClick: () -> Unit,
) {
  Box(Modifier.recomposeHighlighter()) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(
      onClick = {
        expanded = !expanded
      },
    ) {
      Icon(
        imageVector = Icons.Outlined.MoreVert,
        contentDescription = stringResource(id = R.string.more),
      )
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      ListItem(
        modifier = Modifier.clickable(
          onClick = {
            expanded = false
            onSkipSilenceClick()
          },
        ),
        headlineText = {
          Text(text = stringResource(id = R.string.skip_silence))
        },
        trailingContent = {
          Checkbox(
            checked = skipSilence,
            onCheckedChange = {
              expanded = false
              onSkipSilenceClick()
            },
          )
        },
      )
      ListItem(
        modifier = Modifier.clickable(
          onClick = {
            expanded = false
            onVolumeBoostClick()
          },
        ),
        headlineText = {
          Text(text = stringResource(id = R.string.volume_boost))
        },
      )
    }
  }
}

@Composable
private fun CoverRow(
  cover: ImmutableFile?,
  sleepTime: Duration,
  onPlayClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier) {
    Cover(onDoubleClick = onPlayClick, cover = cover)
    if (sleepTime != ZERO) {
      Text(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(top = 8.dp, end = 8.dp)
          .background(
            color = Color(0x7E000000),
            shape = RoundedCornerShape(20.dp),
          )
          .padding(horizontal = 20.dp, vertical = 16.dp),
        text = formatTime(
          timeMs = sleepTime.inWholeMilliseconds,
          durationMs = sleepTime.inWholeMilliseconds,
        ),
        color = Color.White,
      )
    }
  }
}

@Composable
private fun Cover(onDoubleClick: () -> Unit, cover: ImmutableFile?) {
  AsyncImage(
    modifier = Modifier
      .recomposeHighlighter()
      .fillMaxSize()
      .pointerInput(Unit) {
        detectTapGestures(
          onDoubleTap = {
            onDoubleClick()
          },
        )
      }
      .clip(RoundedCornerShape(20.dp)),
    contentScale = ContentScale.Crop,
    model = cover?.file,
    placeholder = painterResource(id = R.drawable.album_art),
    error = painterResource(id = R.drawable.album_art),
    contentDescription = stringResource(id = R.string.cover),
  )
}

@Composable
private fun ChapterRow(
  chapterName: String,
  nextPreviousVisible: Boolean,
  onSkipToNext: () -> Unit,
  onSkipToPrevious: () -> Unit,
  onCurrentChapterClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .recomposeHighlighter()
      .fillMaxWidth()
      .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (nextPreviousVisible) {
      IconButton(onClick = onSkipToPrevious) {
        Icon(
          modifier = Modifier.size(36.dp),
          imageVector = Icons.Outlined.ChevronLeft,
          contentDescription = stringResource(id = R.string.previous_track),
        )
      }
    }
    Text(
      modifier = Modifier
        .weight(1F)
        .clickable(onClick = onCurrentChapterClick)
        .padding(vertical = 16.dp),
      text = chapterName,
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
    )
    if (nextPreviousVisible) {
      IconButton(onClick = onSkipToNext) {
        Icon(
          modifier = Modifier.size(36.dp),
          imageVector = Icons.Outlined.ChevronRight,
          contentDescription = stringResource(id = R.string.next_track),
        )
      }
    }
  }
}

@Composable
private fun PlaybackRow(
  playing: Boolean,
  onPlayClick: () -> Unit,
  onRewindClick: () -> Unit,
  onFastForwardClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .recomposeHighlighter(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    SkipButton(forward = false, onClick = onRewindClick)
    Spacer(modifier = Modifier.size(16.dp))
    FloatingActionButton(
      modifier = Modifier.size(80.dp),
      onClick = onPlayClick,
    ) {
      Icon(
        modifier = Modifier.size(36.dp),
        imageVector = if (playing) {
          Icons.Filled.Pause
        } else {
          Icons.Filled.PlayArrow
        },
        contentDescription = stringResource(id = R.string.play_pause),
      )
    }
    Spacer(modifier = Modifier.size(16.dp))
    SkipButton(forward = true, onClick = onFastForwardClick)
  }
}

@Composable
private fun SkipButton(
  forward: Boolean,
  onClick: () -> Unit,
) {
  Icon(
    modifier = Modifier
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(bounded = false),
        onClick = onClick,
      )
      .size(50.dp)
      .scale(scaleX = if (forward) -1f else 1F, scaleY = 1f),
    imageVector = Icons.Filled.Undo,
    contentDescription = stringResource(
      id = if (forward) {
        R.string.fast_forward
      } else {
        R.string.rewind
      },
    ),
  )
}

@Composable
private fun SliderRow(
  viewState: BookPlayViewState,
  onSeek: (Duration) -> Unit,
) {
  Row(
    modifier = Modifier
      .recomposeHighlighter()
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    var localValue by remember { mutableStateOf(0F) }
    val interactionSource = remember { MutableInteractionSource() }
    val dragging by interactionSource.collectIsDraggedAsState()
    Text(
      text = formatTime(
        timeMs = if (dragging) {
          (viewState.duration * localValue.toDouble()).inWholeMilliseconds
        } else {
          viewState.playedTime.inWholeMilliseconds
        },
        durationMs = viewState.duration.inWholeMilliseconds,
      ),
    )
    Slider(
      modifier = Modifier
        .weight(1F)
        .padding(horizontal = 8.dp),
      interactionSource = interactionSource,
      value = if (dragging) {
        localValue
      } else {
        (viewState.playedTime / viewState.duration).toFloat()
          .coerceIn(0F, 1F)
      },
      onValueChange = {
        localValue = it
      },
      onValueChangeFinished = {
        onSeek(viewState.duration * localValue.toDouble())
      },
    )
    Text(
      text = formatTime(
        timeMs = viewState.duration.inWholeMilliseconds,
        durationMs = viewState.duration.inWholeMilliseconds,
      ),
    )
  }
}

@Composable
@Preview
private fun BookPlayPreview(
  @PreviewParameter(BookPlayViewStatePreviewProvider::class)
  viewState: BookPlayViewState,
) {
  VoiceTheme {
    BookPlayView(
      viewState = viewState,
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
      onCurrentChapterClick = {},
      useLandscapeLayout = false,
    )
  }
}

private class BookPlayViewStatePreviewProvider : PreviewParameterProvider<BookPlayViewState> {
  override val values = sequence {
    val initial = BookPlayViewState(
      chapterName = "My Chapter",
      showPreviousNextButtons = false,
      cover = null,
      duration = 10.minutes,
      playedTime = 3.minutes,
      playing = true,
      skipSilence = true,
      sleepTime = 4.minutes,
      title = "Das Ende der Welt",
    )
    yield(initial)
    yield(
      initial.copy(
        showPreviousNextButtons = !initial.showPreviousNextButtons,
        playing = !initial.playing,
        skipSilence = !initial.skipSilence,
      ),
    )
    yield(initial.copy(chapterName = null))
  }
}
