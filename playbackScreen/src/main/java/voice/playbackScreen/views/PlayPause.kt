package voice.playbackScreen.views

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.common.compose.VoiceColors
import voice.playbackScreen.BookPlayListener
import voice.playbackScreen.R

@Composable
internal fun PlayPause(playing: Boolean, listener: BookPlayListener) {
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
