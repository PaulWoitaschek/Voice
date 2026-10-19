package voice.features.onboarding.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.sqrt

/** Tapping it is just for fun, so it's hidden from accessibility services. */
@Composable
internal fun VoiceIcon(
  clock: () -> Float,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val colors = MaterialTheme.colorScheme
  val scope = rememberCoroutineScope()
  // coming back to the screen shows the icon as it was left
  var shown by rememberSaveable { mutableStateOf(false) }
  val pop = remember { Animatable(if (shown) 1F else 0F) }
  val rise = remember { Animatable(if (shown) 1F else 0F) }
  val bounce = remember { Animatable(0F) }
  val ripple = remember { Animatable(1F) }
  var pressed by remember { mutableStateOf(false) }
  val squish by animateFloatAsState(
    targetValue = if (pressed) 0.9F else 1F,
    animationSpec = spring(dampingRatio = 0.35F, stiffness = Spring.StiffnessMedium),
    label = "iconSquish",
  )
  LaunchedEffect(Unit) {
    if (shown) return@LaunchedEffect
    launch { pop.animateTo(1F, spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessMediumLow)) }
    // the waves start as a flat line and rise once the icon is there, like a voice starting to speak
    delay(200)
    rise.animateTo(1F, spring(dampingRatio = 0.3F, stiffness = Spring.StiffnessMediumLow))
    shown = true
  }
  val wave = remember { Path() }
  val tile = remember { Path() }
  Canvas(
    modifier = modifier
      .clearAndSetSemantics {}
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            pressed = true
            tryAwaitRelease()
            pressed = false
          },
          onTap = {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            scope.launch {
              bounce.animateTo(0F, spring(dampingRatio = 0.3F, stiffness = Spring.StiffnessLow), initialVelocity = 5F)
            }
            scope.launch {
              ripple.snapTo(0F)
              ripple.animateTo(1F, tween(durationMillis = 900, easing = LinearOutSlowInEasing))
            }
          },
        )
      },
  ) {
    val t = clock()
    val side = min(min(size.width, size.height) * 0.6F, 200.dp.toPx())
    val unit = side / VISIBLE_SIZE
    val topLeft = Offset(center.x - side / 2, center.y - side / 2)
    val cornerFraction = 0.25F

    if (ripple.value < 1F) {
      val rippleSide = side * (1F + 0.5F * ripple.value)
      drawRoundRect(
        color = colors.primary,
        topLeft = Offset(center.x - rippleSide / 2, center.y - rippleSide / 2),
        size = Size(rippleSide, rippleSide),
        cornerRadius = CornerRadius(rippleSide * cornerFraction),
        alpha = 0.5F * (1F - ripple.value),
        style = Stroke(width = 3.dp.toPx()),
      )
    }

    val alpha = pop.value.coerceIn(0F, 1F)
    scale(scale = squish * (0.5F + 0.5F * pop.value), pivot = center) {
      val corner = CornerRadius(side * cornerFraction)
      drawRoundRect(colors.primary, topLeft, Size(side, side), corner, alpha = alpha)
      tile.rewind()
      tile.addRoundRect(RoundRect(Rect(topLeft, Size(side, side)), corner))
      clipPath(tile) {
        val amplitude = rise.value * (1F + bounce.value)
        drawWave(
          path = wave,
          topLeft = topLeft,
          unit = unit,
          phase = BACK_PHASE + t * SPEED,
          amplitude = amplitude,
          drop = BACK_DROP,
          color = colors.onPrimary,
          alpha = alpha,
        )
        drawWave(
          path = wave,
          topLeft = topLeft,
          unit = unit,
          phase = FRONT_PHASE + t * SPEED,
          amplitude = amplitude,
          drop = 0F,
          color = colors.onPrimary,
          alpha = alpha,
        )
      }
    }
  }
}

private fun DrawScope.drawWave(
  path: Path,
  topLeft: Offset,
  unit: Float,
  phase: Float,
  amplitude: Float,
  drop: Float,
  color: Color,
  alpha: Float,
) {
  path.rewind()
  for (i in 0..SAMPLES) {
    val x = VISIBLE_START + VISIBLE_SIZE * i / SAMPLES
    val y = CENTER + (waveY(x + phase) - CENTER) * amplitude + drop
    val point = topLeft + Offset((x - VISIBLE_START) * unit, (y - VISIBLE_START) * unit)
    if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
  }
  drawPath(path, color, alpha = alpha, style = Stroke(width = STROKE_WIDTH * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// the wave of the launcher icon, in the units of its vector: a round dip, a slope up, a round crest and a slope down
private fun waveY(x: Float): Float {
  val u = ((x % PERIOD) + PERIOD) % PERIOD
  val crestStart = CHORD + SLOPE
  return when {
    u < CHORD -> BOTTOM - ARC_OFFSET + sqrt(RADIUS * RADIUS - (u - CHORD / 2) * (u - CHORD / 2))
    u < crestStart -> BOTTOM - (u - CHORD) / SLOPE * (BOTTOM - TOP)
    u < crestStart + CHORD -> TOP + ARC_OFFSET - sqrt(RADIUS * RADIUS - (u - crestStart - CHORD / 2) * (u - crestStart - CHORD / 2))
    else -> TOP + (u - crestStart - CHORD) / SLOPE * (BOTTOM - TOP)
  }
}

private const val ICON_SIZE = 108F
private const val CENTER = ICON_SIZE / 2

// the part of the vector the Play Store icon shows
private const val VISIBLE_SIZE = ICON_SIZE / 1.3F
private const val VISIBLE_START = CENTER - VISIBLE_SIZE / 2
private const val SAMPLES = 160
private const val STROKE_WIDTH = 2F
private const val SPEED = 8F
private const val RADIUS = 11.9F
private const val CHORD = 22.12F
private const val SLOPE = 7.92F
private const val PERIOD = 2 * (CHORD + SLOPE)
private const val TOP = 44F
private const val BOTTOM = 64F
private val ARC_OFFSET = sqrt(RADIUS * RADIUS - CHORD * CHORD / 4)

// where the waves of the launcher icon start, so the still icon looks just like it
private const val FRONT_PHASE = 21.26F
private const val BACK_PHASE = 11.91F
private const val BACK_DROP = 0.5F
