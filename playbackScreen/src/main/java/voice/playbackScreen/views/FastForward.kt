package voice.playbackScreen.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.playbackScreen.BookPlayListener
import voice.playbackScreen.R

@Composable
internal fun FastForward(listener: BookPlayListener) {
  Column(Modifier.padding(vertical = 8.dp)) {
    IconButton(
      onClick = {
        listener.fastForward()
      }
    ) {
      Icon(
        imageVector = Icons.Default.FastForward,
        contentDescription = stringResource(R.string.fast_forward)
      )
    }
  }
}
