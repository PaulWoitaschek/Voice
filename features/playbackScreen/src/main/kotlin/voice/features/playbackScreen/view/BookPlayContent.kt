@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen.view

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
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
    BookProgress(
      viewState = viewState,
      modifier = Modifier
        .fillMaxWidth()
        .entrance(entrance, 1),
    )
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
    Spacer(Modifier.height(if (compact) 12.dp else 20.dp))
    ActionToolbar(
      viewState = viewState,
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
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
        .padding(horizontal = 24.dp)
        .padding(bottom = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      cover(
        Modifier
          .weight(1F)
          .fillMaxWidth()
          .padding(vertical = 8.dp),
      )
      Spacer(Modifier.height(16.dp))
      controls(104.dp, false)
    }
  }
}

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
