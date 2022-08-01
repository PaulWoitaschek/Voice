package voice.bookOverview.views

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.bookOverview.R

@Composable
internal fun PlayButton(playing: Boolean, onClick: () -> Unit) {
  FloatingActionButton(onClick = onClick) {
    Icon(
      painter = rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(
          id = R.drawable.avd_pause_to_play,
        ),
        atEnd = !playing,
      ),
      contentDescription = stringResource(R.string.play_pause),
    )
  }
}
