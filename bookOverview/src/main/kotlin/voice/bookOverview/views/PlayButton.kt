package voice.bookOverview.views

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.common.compose.rememberPlayIconPainter
import voice.strings.R as StringsR

@Composable
internal fun PlayButton(playing: Boolean, onClick: () -> Unit) {
  FloatingActionButton(onClick = onClick) {
    Icon(
      painter = rememberPlayIconPainter(playing = playing),
      contentDescription = stringResource(StringsR.string.play_pause),
    )
  }
}
