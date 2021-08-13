package voice.playbackScreen.views

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.playbackScreen.BookPlayListener
import voice.playbackScreen.R

@Composable
internal fun PlayPause(listener: BookPlayListener) {
  IconButton(
    onClick = {
      listener.playPause()
    }
  ) {
    Icon(
      imageVector = Icons.Default.PlayCircleOutline,
      contentDescription = stringResource(R.string.play_pause)
    )
  }
}
