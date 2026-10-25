package voice.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale

/**
 * False when animations are turned off in the system settings. Then animations jump to their end on the next frame,
 * so motion that only plays out and goes away, like a burst, would flash for that frame. Leave it out instead.
 */
@Composable
fun animationsEnabled(): Boolean {
  val motionDurationScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]
  return (motionDurationScale?.scaleFactor ?: 1F) > 0F
}
