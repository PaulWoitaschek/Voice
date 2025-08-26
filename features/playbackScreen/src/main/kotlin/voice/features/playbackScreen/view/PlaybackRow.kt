package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import voice.core.common.compose.PlayButton

@Composable
internal fun PlaybackRow(
  playing: Boolean,
  onPlayClick: () -> Unit,
  onRewindClick: () -> Unit,
  onFastForwardClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    SkipButton(forward = false, onClick = onRewindClick)
    Spacer(modifier = Modifier.size(16.dp))

    PlayButton(playing = playing, fabSize = 80.dp, iconSize = 36.dp, onPlayClick = onPlayClick)
    Spacer(modifier = Modifier.size(16.dp))
    SkipButton(forward = true, onClick = onFastForwardClick)
  }
}
