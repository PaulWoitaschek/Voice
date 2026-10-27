package voice.core.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

private val StrokeWidth = 5.dp

@Composable
fun ProgressWave(
  progress: Float,
  playing: Boolean,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.primary,
  trackColor: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
  val clock = rememberAnimationClock(running = playing)
  val amplitude by animateWaveAmplitude(wavy = playing)
  Spacer(
    modifier = modifier
      .height(10.dp)
      .semantics(mergeDescendants = true) { progressBarRangeInfo = ProgressBarRangeInfo(current = progress, range = 0F..1F) }
      // it redraws every frame while playing, its own layer keeps that from re-recording what surrounds it
      .graphicsLayer()
      .drawWithCache {
        val path = Path()
        onDrawBehind {
          drawProgressWave(
            path = path,
            fraction = progress,
            amplitude = amplitude,
            seconds = clock.value,
            color = color,
            trackColor = trackColor,
            inset = StrokeWidth.toPx() / 2,
          )
        }
      },
  )
}

@Composable
fun animateWaveAmplitude(wavy: Boolean): State<Float> = animateFloatAsState(
  targetValue = if (wavy) 1F else 0F,
  animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
  label = "waveAmplitude",
)

fun DrawScope.drawProgressWave(
  path: Path,
  fraction: Float,
  amplitude: Float,
  seconds: Float,
  color: Color,
  trackColor: Color,
  inset: Float,
  thumbWidth: Float = 0F,
  thumbHeight: Float = 0F,
) {
  val strokeWidth = StrokeWidth.toPx()
  val gap = 5.dp.toPx()
  val centerY = size.height / 2
  val end = size.width - inset
  val thumbX = inset + (end - inset) * fraction
  val waveAmplitude = 1.75.dp.toPx() * amplitude
  val wavelength = 44.dp.toPx()
  val phase = seconds * 2F * PI.toFloat() * 0.2F
  val step = 1.5.dp.toPx()

  val hasThumb = thumbWidth > 0F
  // without a thumb, the gap only comes before the track, so a book that hasn't started shows the whole track and one
  // that has shows at least a dot
  val activeEnd = if (hasThumb) thumbX - thumbWidth / 2 - gap else thumbX
  if (activeEnd > inset) {
    path.reset()
    var x = inset
    path.moveTo(x, centerY + waveAmplitude * sin(phase))
    while (x < activeEnd) {
      x = min(x + step, activeEnd)
      val angle = (x - inset) / wavelength * 2F * PI.toFloat() + phase
      path.lineTo(x, centerY + waveAmplitude * sin(angle))
    }
    drawPath(
      path = path,
      color = color,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
  }

  val inactiveStart = when {
    hasThumb -> thumbX + thumbWidth / 2 + gap
    fraction > 0F -> thumbX + strokeWidth + gap
    else -> inset
  }
  if (inactiveStart < end) {
    drawLine(
      color = trackColor,
      start = Offset(inactiveStart, centerY),
      end = Offset(end, centerY),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round,
    )
    drawCircle(color = color, radius = 1.5.dp.toPx(), center = Offset(end, centerY))
  }

  if (hasThumb) {
    drawRoundRect(
      color = color,
      topLeft = Offset(thumbX - thumbWidth / 2, centerY - thumbHeight / 2),
      size = Size(thumbWidth, thumbHeight),
      cornerRadius = CornerRadius(thumbWidth / 2),
    )
  }
}
