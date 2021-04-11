package voice.playbackScreen.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.playbackScreen.BookPlayListener
import voice.playbackScreen.R

@Composable
internal fun Rewind(listener: BookPlayListener) {
  Column(Modifier.padding(vertical = 8.dp)) {
    IconButton(
      onClick = {
        listener.rewind()
      }
    ) {
      Icon(
        imageVector = Icons.Default.FastRewind,
        contentDescription = stringResource(R.string.rewind)
      )
    }
  }
}
