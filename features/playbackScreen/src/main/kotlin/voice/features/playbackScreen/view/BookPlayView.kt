package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import voice.core.data.BookId
import voice.core.ui.AuroraBackground
import voice.core.ui.CoverTheme
import voice.core.ui.VoiceTheme
import voice.core.ui.rememberAnimationClock
import voice.features.playbackScreen.BookPlayViewState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun BookPlayView(
  viewState: BookPlayViewState,
  bookId: BookId,
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
  onBookActionsClick: () -> Unit,
  onCurrentChapterClick: () -> Unit,
  onJumpBack: (id: Long) -> Unit,
  onJumpBackExpire: (id: Long) -> Unit,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
  val clock = rememberAnimationClock(running = viewState.playing)
  BoxWithConstraints(Modifier.fillMaxSize()) {
    // split screen panes are often wider than tall, but too narrow or too short for a cover next to the controls
    val sideBySide = maxWidth > maxHeight && maxWidth >= 600.dp && maxHeight >= 360.dp
    AuroraBackground(
      clock = { clock.value },
      showStars = viewState.sleepTimerState is BookPlayViewState.SleepTimerViewState.Enabled,
      modifier = Modifier.fillMaxSize(),
    )
    Scaffold(
      containerColor = Color.Transparent,
      contentColor = MaterialTheme.colorScheme.onSurface,
      snackbarHost = {
        SnackbarHost(hostState = snackbarHostState)
      },
      topBar = {
        if (!sideBySide) {
          PlaybackTopBar(
            showChapters = viewState.showPreviousNextButtons,
            onCloseClick = onCloseClick,
            onChaptersClick = onCurrentChapterClick,
            onBookActionsClick = onBookActionsClick,
          )
        }
      },
      content = { contentPadding ->
        BookPlayContent(
          contentPadding = contentPadding,
          viewState = viewState,
          bookId = bookId,
          clock = { clock.value },
          sideBySide = sideBySide,
          onPlayClick = onPlayClick,
          onRewindClick = onRewindClick,
          onFastForwardClick = onFastForwardClick,
          onSeek = onSeek,
          onSkipToNext = onSkipToNext,
          onSkipToPrevious = onSkipToPrevious,
          onCurrentChapterClick = onCurrentChapterClick,
          onSleepTimerClick = onSleepTimerClick,
          onSpeedChangeClick = onSpeedChangeClick,
          onBookmarkClick = onBookmarkClick,
          onBookmarkLongClick = onBookmarkLongClick,
          onSkipSilenceClick = onSkipSilenceClick,
          onVolumeBoostClick = onVolumeBoostClick,
          onCloseClick = onCloseClick,
          onBookActionsClick = onBookActionsClick,
          onJumpBack = onJumpBack,
          onJumpBackExpire = onJumpBackExpire,
        )
      },
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
    CoverTheme(cover = viewState.cover) {
      BookPlayView(
        viewState = viewState,
        bookId = BookId("preview"),
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

private class BookPlayViewStatePreviewProvider : PreviewParameterProvider<BookPlayViewState> {
  override val values = sequence {
    val initial = BookPlayViewState(
      chapterName = "The Signal",
      showPreviousNextButtons = true,
      cover = null,
      duration = 42.minutes,
      playedTime = 17.minutes,
      playing = true,
      skipSilence = true,
      sleepTimerState = BookPlayViewState.SleepTimerViewState.Disabled,
      title = "Das Ende der Welt",
      author = "Daniel Hartwell",
      playbackSpeed = 1.25F,
      volumeBoostActive = false,
      chapterNumber = 5,
      chapterCount = 12,
      bookPlayedTime = 4.hours + 12.minutes,
      bookDuration = 9.hours + 30.minutes,
      chapterSegments = List(12) { 1F / 12 },
      skipSeconds = 20,
      jumpBack = BookPlayViewState.JumpBackViewState(
        id = 1,
        time = "34:18",
        chapterNumber = null,
        elapsed = 3.seconds,
      ),
    )
    yield(initial)
    yield(
      initial.copy(
        playing = false,
        skipSilence = false,
        playbackSpeed = 1F,
        sleepTimerState = BookPlayViewState.SleepTimerViewState.Enabled.WithDuration(14.minutes),
      ),
    )
    yield(
      initial.copy(
        chapterName = null,
        showPreviousNextButtons = false,
        duration = initial.bookDuration,
        playedTime = initial.bookPlayedTime,
        chapterCount = 1,
        chapterNumber = 1,
        chapterSegments = listOf(1F),
        jumpBack = null,
      ),
    )
  }
}
