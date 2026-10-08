@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import voice.core.ui.icons.VoiceIcons
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/** A heart beating in a slowly turning burst. Asks for support without saying a word. */
@Composable
fun BeatingHeart(
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
  size: Dp = 64.dp,
) {
  val clock = rememberAnimationClock(running = true)
  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      Modifier
        .matchParentSize()
        .graphicsLayer { rotationZ = clock.value * 15F }
        .background(containerColor, MaterialShapes.SoftBurst.toShape()),
    )
    Icon(
      modifier = Modifier
        .size(size * 0.47F)
        .graphicsLayer {
          val scale = 1F + 0.15F * heartbeat(clock.value)
          scaleX = scale
          scaleY = scale
        },
      imageVector = VoiceIcons.Favorite,
      contentDescription = null,
      tint = contentColor,
    )
  }
}

/** Two quick beats, then a rest. */
private fun heartbeat(seconds: Float): Float {
  val phase = seconds % 1.4F
  fun beat(start: Float): Float {
    val local = (phase - start) / 0.16F
    return if (local in 0F..1F) sin(local * PI.toFloat()) else 0F
  }
  return max(beat(0F), beat(0.24F) * 0.7F)
}
