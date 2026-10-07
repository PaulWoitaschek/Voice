package voice.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Three little bars bouncing to the audio while it plays. */
@Composable
fun NowPlayingBars(
  playing: Boolean,
  color: Color,
  modifier: Modifier = Modifier,
) {
  // unlike an infinite transition, the clock stops requesting frames while paused
  val clock = rememberAnimationClock(running = playing)
  val amplitude by animateFloatAsState(if (playing) 1F else 0F, label = "barsAmplitude")
  Canvas(modifier = modifier.size(20.dp)) {
    val barWidth = size.width / 5
    val phase = clock.value * 2F * PI.toFloat() / 1.1F
    repeat(3) { index ->
      val wave = 0.5F + 0.5F * sin(phase * (1 + index * 0.35F) + index * 2.1F)
      val height = size.height * (0.3F + 0.7F * amplitude * wave)
      drawRoundRect(
        color = color,
        topLeft = Offset(index * 2 * barWidth, (size.height - height) / 2),
        size = Size(barWidth, height),
        cornerRadius = CornerRadius(barWidth / 2),
      )
    }
  }
}
