package voice.playbackScreen.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.UUID
import voice.common.compose.VoiceTheme
import voice.playbackScreen.BookPlayListener
import voice.playbackScreen.BookPlayViewState
import voice.playbackScreen.R
import kotlin.time.Duration

@Composable
internal fun BookPlayView(viewState: BookPlayViewState, listener: BookPlayListener) {
  VoiceTheme {
    Box {
      Column {
        BookPlayCover(
          modifier = Modifier
            .weight(1F)
            .fillMaxSize(),
          cover = viewState.cover
        )
        Surface(elevation = 8.dp) {
          Column {
            Spacer(modifier = Modifier.size(16.dp))
            Title(
              Modifier
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally),
              viewState
            )
            Spacer(Modifier.size(8.dp))
            if (viewState.chapterName != null) {
              ChapterRow(viewState.chapterName, listener)
            }
            PlaybackSlider(viewState, listener)
            PlayRow(listener)
          }
        }
      }

      TopAppBar(
        title = { },
        actions = {
          AppBarIcons()
        },
        navigationIcon = {
          CloseIcon(listener)
        }
      )
    }
  }
}

@Composable
private fun AppBarIcons() {
  BookmarkIcon()
}

@Composable
private fun BookmarkIcon() {
  IconButton(
    onClick = {
    },
    content = {
      Icon(
        imageVector = Icons.Default.Bookmark,
        contentDescription = stringResource(R.string.bookmark)
      )
    }
  )
}

@Composable
private fun CloseIcon(listener: BookPlayListener) {
  IconButton(
    onClick = {
      listener.close()
    }
  ) {
    Icon(
      imageVector = Icons.Default.Close,
      contentDescription = stringResource(R.string.close),
    )
  }
}

@Composable
private fun PlayRow(listener: BookPlayListener) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center
  ) {
    SleepTimerIcon(listener)
    Rewind(listener)
    PlayPause(listener)
    FastForward(listener)
    PlaybackSpeedIcon(listener)
  }
}

@Composable
private fun PlaybackSpeedIcon(listener: BookPlayListener) {
  IconButton(
    onClick = {
      listener.close()
    }
  ) {
    Icon(
      imageVector = Icons.Default.Speed,
      contentDescription = stringResource(R.string.action_sleep)
    )
  }
}

@Composable
private fun SleepTimerIcon(listener: BookPlayListener) {
  IconButton(
    onClick = {
      listener.close()
    }
  ) {
    Icon(
      imageVector = Icons.Default.AlarmAdd,
      contentDescription = stringResource(R.string.action_sleep)
    )
  }
}

@Composable
private fun Title(
  modifier: Modifier,
  viewState: BookPlayViewState
) {
  Text(
    modifier = modifier,
    text = viewState.title,
    style = MaterialTheme.typography.h5,
    textAlign = TextAlign.Center
  )
}

@Preview
@Composable
private fun BookPlayViewPreview() {
  val viewState = BookPlayViewState(
    chapterName = "Chapter 1",
    showPreviousNextButtons = true,
    title = "Perry Hotter",
    sleepTime = Duration.ZERO,
    playedTime = Duration.seconds(100),
    cover = voice.playbackScreen.BookPlayCover(
      "Perry Hotter", UUID.randomUUID()
    ),
    duration = Duration.seconds(200),
    playing = true,
    skipSilence = false
  )
  val listener = object : BookPlayListener {
    override fun close() {}
    override fun fastForward() {}
    override fun rewind() {}
    override fun previousTrack() {}
    override fun nextTrack() {}
    override fun playPause() {}
    override fun seekTo(milliseconds: Long) {}
  }
  BookPlayView(viewState = viewState, listener = listener)
}
