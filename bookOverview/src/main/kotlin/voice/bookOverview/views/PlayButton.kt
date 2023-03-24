package voice.bookOverview.views

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.common.R as CommonR
import voice.strings.R as StringsR

@Composable
internal fun PlayButton(playing: Boolean, onClick: () -> Unit) {
  FloatingActionButton(onClick = onClick) {
    Icon(
      painter = rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(
          id = CommonR.drawable.avd_pause_to_play,
        ),
        atEnd = !playing,
      ),
      contentDescription = stringResource(StringsR.string.play_pause),
    )
  }
}
