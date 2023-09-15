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
import androidx.compose.ui.unit.dp
import voice.common.compose.ImmutableFile
import voice.common.formatTime
import kotlin.time.Duration

@Composable
internal fun CoverRow(
  cover: ImmutableFile?,
  sleepTime: Duration,
  onPlayClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier) {
    Cover(onDoubleClick = onPlayClick, cover = cover)
    if (sleepTime != Duration.ZERO) {
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
