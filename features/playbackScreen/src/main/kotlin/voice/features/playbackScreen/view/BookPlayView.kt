package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.Box
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
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.ui.AuroraBackground
import voice.core.ui.BookBarPin
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
  onJumpBack: () -> Unit,
  onJumpBackExpire: () -> Unit,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
  val clock = rememberAnimationClock(running = viewState.playing)
  Box(Modifier.fillMaxSize()) {
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
        if (!useLandscapeLayout) {
          PlaybackTopBar(
            showChapters = viewState.showPreviousNextButtons,
            onCloseClick = onCloseClick,
            onChaptersClick = onCurrentChapterClick,
          )
        }
      },
      content = { contentPadding ->
        BookPlayContent(
          contentPadding = contentPadding,
          viewState = viewState,
          bookId = bookId,
          clock = { clock.value },
          useLandscapeLayout = useLandscapeLayout,
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
        onCurrentChapterClick = {},
        onJumpBack = {},
        onJumpBackExpire = {},
        useLandscapeLayout = false,
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
      bookmarkPins = listOf(
        BookBarPin(position = 0.12F, kind = Bookmark.Kind.Favorite, setBySleepTimer = false),
        BookBarPin(position = 0.38F, kind = Bookmark.Kind.Quote, setBySleepTimer = false),
        BookBarPin(position = 0.39F, kind = Bookmark.Kind.Note, setBySleepTimer = true),
        BookBarPin(position = 0.8F, kind = Bookmark.Kind.Revisit, setBySleepTimer = false),
      ),
      poppedPin = null,
      jumpBack = BookPlayViewState.JumpBackViewState(
        id = 1,
        time = "34:18",
        chapterNumber = null,
        remaining = 10.seconds,
        visibleFor = 10.seconds,
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
        chapterCount = 1,
        chapterNumber = 1,
        chapterSegments = listOf(1F),
        bookmarkPins = emptyList(),
        jumpBack = null,
      ),
    )
  }
}
