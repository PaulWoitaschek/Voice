package voice.playbackScreen.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.rememberImagePainter
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
      Column {
        Image(
          modifier = Modifier
            .weight(1F)
            .fillMaxSize(),
          contentScale = ContentScale.Crop,
          painter = rememberImagePainter(
            data = viewState.cover.file(LocalContext.current),
            builder = {
              error(viewState.cover.placeholder(LocalContext.current))
            }
          ),
          contentDescription = null
        )
        PlaybackSlider(viewState, listener)
        Row {
          Rewind(listener)
          PlayPause(listener)
          FastForward(listener)
        }
      }
    }
  }
}
