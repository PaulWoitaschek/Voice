package voice.features.playbackScreen.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.first

/**
 * A clock in seconds that only ticks while audio plays. Its speed eases in and out, so everything
 * driven by it (the seek bar wave, the aurora) glides to a halt instead of freezing abruptly.
 * When paused it stops requesting frames entirely.
 */
@Composable
internal fun rememberPlaybackClock(playing: Boolean): State<Float> {
  val time = remember { mutableFloatStateOf(0F) }
  val speed = animateFloatAsState(
    targetValue = if (playing) 1F else 0F,
    animationSpec = tween(durationMillis = 1200),
    label = "clockSpeed",
  )
  LaunchedEffect(time, speed) {
    while (true) {
      snapshotFlow { speed.value > 0F }.first { it }
      var lastFrame = withFrameNanos { it }
      while (speed.value > 0F) {
        withFrameNanos { frame ->
          time.floatValue += (frame - lastFrame) / 1_000_000_000F * speed.value
          lastFrame = frame
        }
      }
    }
  }
  return time
}
