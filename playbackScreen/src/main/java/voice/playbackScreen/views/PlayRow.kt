package voice.playbackScreen.views

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.common.compose.VoiceColors
import voice.playbackScreen.BookPlayListener
import voice.playbackScreen.R

@Composable
internal fun PlayRow(playing: Boolean, listener: BookPlayListener) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center
  ) {
    SleepTimerIcon(modifier = Modifier, listener)
    Rewind(listener)
    PlayPause(playing, listener)
    FastForward(listener)
    PlaybackSpeedIcon(listener)
  }
}

@Preview
@Composable
private fun PreviewPlayRow() {
  PlayRow(playing = true, listener = NoOpBookPlayListener)
}

@Composable
private fun SleepTimerIcon(modifier: Modifier = Modifier, listener: BookPlayListener) {
  IconButton(
    modifier = modifier,
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
private fun Rewind(listener: BookPlayListener) {
  IconButton(
    modifier = Modifier, // .size(80.dp),
    onClick = {
      listener.rewind()
    }
  ) {
    Icon(
      modifier = Modifier.size(44.dp),
      imageVector = Icons.Default.FastRewind,
      contentDescription = stringResource(R.string.rewind)
    )
  }
}

@Composable
private fun PlayPause(playing: Boolean, listener: BookPlayListener) {
  Surface(
    modifier = Modifier.size(80.dp),
    elevation = 8.dp,
    color = VoiceColors.Red700,
    shape = CircleShape,
    onClick = {
      listener.playPause()
    }
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      val playToPause = animatedVectorResource(id = R.drawable.avd_play_to_pause)
      Icon(
        modifier = Modifier
          .align(Alignment.Center)
          .size(48.dp),
        painter = playToPause.painterFor(atEnd = playing),
        tint = Color.White,
        contentDescription = stringResource(R.string.play_pause)
      )
    }
  }
}

@Composable
private fun FastForward(listener: BookPlayListener) {
  IconButton(
    onClick = {
      listener.fastForward()
    }
  ) {
    Icon(
      modifier = Modifier.size(44.dp),
      imageVector = Icons.Default.FastForward,
      contentDescription = stringResource(R.string.fast_forward)
    )
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
