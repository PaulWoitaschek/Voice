package voice.playbackScreen.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.ph1b.audiobook.features.bookPlaying.BookPlayViewState
import voice.playbackScreen.BookPlayListener
import voice.playbackScreen.R

@Composable
internal fun BookPlayView(viewState: BookPlayViewState, listener: BookPlayListener) {
  MaterialTheme {
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(viewState.title)
          },
          actions = {
            IconButton(
              onClick = {
              },
              content = {
                Icon(
                  imageVector = Icons.Default.Favorite,
                  contentDescription = stringResource(R.string.pref_support_title)
                )
              }
            )
          },
          navigationIcon = {
            IconButton(
              onClick = {
                listener.close()
              }
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close)
              )
            }
          }
        )
      }
    ) {
      Row {

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
    }
  }
}
