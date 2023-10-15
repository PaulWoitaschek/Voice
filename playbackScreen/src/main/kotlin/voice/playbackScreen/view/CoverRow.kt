package voice.playbackScreen.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.common.compose.ImmutableFile
import voice.common.formatTime
import voice.playbackScreen.BookPlayViewState
import voice.strings.R

@Composable
internal fun CoverRow(
  cover: ImmutableFile?,
  sleepTimer: BookPlayViewState.SleepTimerViewState?,
  onPlayClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier) {
    Cover(onDoubleClick = onPlayClick, cover = cover)
    if (sleepTimer != null) {
      Text(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(top = 8.dp, end = 8.dp)
          .background(
            color = Color(0x7E000000),
            shape = RoundedCornerShape(20.dp),
          )
          .padding(horizontal = 20.dp, vertical = 16.dp),
        text = when (sleepTimer) {
          BookPlayViewState.SleepTimerViewState.SleepAtEndOfChapter -> stringResource(R.string.end_of_chapter)
          is BookPlayViewState.SleepTimerViewState.SleepAfterDuration -> formatTime(
            timeMs = sleepTimer.remaining.inWholeMilliseconds,
            durationMs = sleepTimer.remaining.inWholeMilliseconds,
          )
        },
        color = Color.White,
      )
    }
  }
}
