package voice.playbackScreen.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.playbackScreen.BookPlayListener
import voice.playbackScreen.R

@Composable
internal fun PlayPause(listener: BookPlayListener) {
  Column(Modifier.padding(vertical = 8.dp)) {
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
}
