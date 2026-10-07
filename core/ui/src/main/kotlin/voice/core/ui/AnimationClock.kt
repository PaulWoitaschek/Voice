package voice.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.flow.first

/**
 * A clock in seconds that only ticks while [running], e.g. while audio plays. Its speed eases in and
 * out, so everything driven by it (the seek bar wave, the aurora) glides to a halt instead of
 * freezing abruptly. When stopped, or with animations turned off, it stops requesting frames entirely.
 */
@Composable
fun rememberAnimationClock(running: Boolean): State<Float> {
  val time = remember { mutableFloatStateOf(0F) }
  val speed = animateFloatAsState(
    targetValue = if (running) 1F else 0F,
    animationSpec = tween(durationMillis = 1200),
    label = "clockSpeed",
  )
  LaunchedEffect(time, speed) {
    // zero when animations are turned off in the system settings
    val motionDurationScale = coroutineContext[MotionDurationScale]
    fun ticking() = speed.value > 0F && (motionDurationScale?.scaleFactor ?: 1F) > 0F
    while (true) {
      snapshotFlow { ticking() }.first { it }
      var lastFrame = withFrameNanos { it }
      while (ticking()) {
        withFrameNanos { frame ->
          time.floatValue += (frame - lastFrame) / 1_000_000_000F * speed.value
          lastFrame = frame
        }
      }
    }
  }
  return time
}
