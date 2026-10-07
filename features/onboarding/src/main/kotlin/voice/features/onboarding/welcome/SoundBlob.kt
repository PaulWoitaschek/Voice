@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.onboarding.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.graphics.shapes.toPath as toAndroidPath

/**
 * Voice's mascot of sorts: a blob that keeps morphing through Material shapes while an equalizer
 * dances inside it, sound rings ripple outwards and a few tiny shapes orbit it.
 *
 * Tapping it (just for fun, so it's hidden from accessibility services) squishes it and jumps to the
 * next shape right away.
 */
@Composable
internal fun SoundBlob(
  clock: () -> Float,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val colors = MaterialTheme.colorScheme
  val morphs = remember {
    BLOB_SHAPES.indices.map { index -> Morph(BLOB_SHAPES[index], BLOB_SHAPES[(index + 1) % BLOB_SHAPES.size]) }
  }
  val satellitePaths = remember { SATELLITES.map { it.polygon.toAndroidPath().asComposePath() } }
  val satelliteColors = listOf(colors.tertiary, colors.secondary, colors.tertiary, colors.primary)
  var shapeIndex by remember { mutableIntStateOf(0) }
  val morphProgress = remember { Animatable(0F) }
  val taps = remember { Channel<Unit>(Channel.CONFLATED) }
  // every tap and morph sends out a ring, which the clock carries away
  var pulseStart by remember { mutableStateOf(Float.NEGATIVE_INFINITY) }
  var pressed by remember { mutableStateOf(false) }
  val squish by animateFloatAsState(
    targetValue = if (pressed) 0.86F else 1F,
    animationSpec = spring(dampingRatio = 0.35F, stiffness = Spring.StiffnessMedium),
    label = "blobSquish",
  )
  val currentClock by rememberUpdatedState(clock)
  LaunchedEffect(morphs) {
    val animationsOff = coroutineContext[MotionDurationScale]?.scaleFactor == 0F
    while (true) {
      val tapped = withTimeoutOrNull(MORPH_INTERVAL_MS) { taps.receive() } != null
      // with animations off the blob holds still, unless it's tapped
      if (!tapped && animationsOff) continue
      pulseStart = currentClock()
      morphProgress.animateTo(1F, spring(dampingRatio = 0.55F, stiffness = Spring.StiffnessLow))
      shapeIndex = (shapeIndex + 1) % BLOB_SHAPES.size
      morphProgress.snapTo(0F)
    }
  }
  val blobPath = remember { Path() }
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
            taps.trySend(Unit)
          },
        )
      },
  ) {
    val t = clock()
    val center = this.center
    val radius = min(min(size.width, size.height) * 0.27F, 132.dp.toPx())

    drawSoundRings(t = t, pulseStart = pulseStart, center = center, radius = radius, color = colors.primary)

    // orbiting shapes behind the blob are drawn first and a bit smaller, which fakes some depth
    val satellites = SATELLITES.mapIndexed { index, satellite ->
      val angle = t * satellite.speed + satellite.phase
      val depth = sin(angle)
      Triple(index, angle, depth)
    }
    satellites.filter { it.third < 0F }.forEach { (index, angle, depth) ->
      drawSatellite(index, angle, depth, t, center, radius, satellitePaths[index], satelliteColors[index])
    }

    morphs[shapeIndex].toPath(progress = morphProgress.value, path = blobPath)
    val blobSize = radius * 2F * squish * (1F + 0.03F * sin(t * 2.2F))
    translate(left = center.x - blobSize / 2, top = center.y - blobSize / 2) {
      rotate(degrees = t * 14F, pivot = Offset(blobSize / 2, blobSize / 2)) {
        scale(scaleX = blobSize, scaleY = blobSize, pivot = Offset.Zero) {
          drawPath(path = blobPath, color = colors.primary)
        }
      }
    }
    drawEqualizer(t = t, center = center, radius = radius * squish, color = colors.onPrimary)

    satellites.filter { it.third >= 0F }.forEach { (index, angle, depth) ->
      drawSatellite(index, angle, depth, t, center, radius, satellitePaths[index], satelliteColors[index])
    }
  }
}

private fun DrawScope.drawSoundRings(
  t: Float,
  pulseStart: Float,
  center: Offset,
  radius: Float,
  color: Color,
) {
  val maxRadius = min(size.width, size.height) / 2F
  // three calm rings that keep rippling outwards
  repeat(3) { ring ->
    val phase = ((t * 0.35F) + ring / 3F) % 1F
    val ringRadius = radius + (maxRadius - radius) * phase
    drawCircle(
      color = color,
      radius = ringRadius,
      center = center,
      alpha = 0.22F * (1F - phase),
      style = Stroke(width = 2.dp.toPx()),
    )
  }
  // plus a bold one whenever the blob changes its shape
  val pulse = (t - pulseStart) / 0.9F
  if (pulse in 0F..1F) {
    drawCircle(
      color = color,
      radius = radius + (maxRadius - radius) * pulse,
      center = center,
      alpha = 0.5F * (1F - pulse),
      style = Stroke(width = (6F * (1F - pulse) + 1F).dp.toPx()),
    )
  }
}

private fun DrawScope.drawEqualizer(
  t: Float,
  center: Offset,
  radius: Float,
  color: Color,
) {
  val barCount = 5
  val barWidth = radius * 0.13F
  val gap = radius * 0.09F
  val maxHeight = radius * 0.78F
  val totalWidth = barCount * barWidth + (barCount - 1) * gap
  repeat(barCount) { bar ->
    // a few overlaid waves per bar, so they bounce like music rather than in lockstep
    val level = 0.5F +
      0.3F * sin(t * (5.1F + bar * 0.7F) + bar * 1.3F) +
      0.2F * sin(t * (8.3F - bar * 0.9F) + bar * 2.1F)
    val height = maxHeight * (0.25F + 0.75F * level.coerceIn(0F, 1F))
    val x = center.x - totalWidth / 2 + bar * (barWidth + gap)
    drawRoundRect(
      color = color,
      topLeft = Offset(x, center.y - height / 2),
      size = Size(barWidth, height),
      cornerRadius = CornerRadius(barWidth / 2),
    )
  }
}

private fun DrawScope.drawSatellite(
  index: Int,
  angle: Float,
  depth: Float,
  t: Float,
  center: Offset,
  radius: Float,
  path: Path,
  color: Color,
) {
  val satellite = SATELLITES[index]
  val orbitX = radius * satellite.orbit * 1.55F
  val orbitY = radius * satellite.orbit * 0.42F
  // a tilted orbit, so they swing around the blob instead of circling flat
  val x = center.x + cos(angle) * orbitX
  val y = center.y + sin(angle) * orbitY - cos(angle) * radius * 0.25F
  val sizePx = satellite.size.dp.toPx() * (0.75F + 0.25F * (depth + 1F))
  translate(left = x - sizePx / 2, top = y - sizePx / 2) {
    rotate(degrees = t * satellite.spin, pivot = Offset(sizePx / 2, sizePx / 2)) {
      scale(scaleX = sizePx, scaleY = sizePx, pivot = Offset.Zero) {
        drawPath(path = path, color = color, alpha = 0.65F + 0.35F * abs(depth))
      }
    }
  }
}

private class Satellite(
  val polygon: RoundedPolygon,
  val size: Float,
  val orbit: Float,
  val speed: Float,
  val phase: Float,
  val spin: Float,
)

private val BLOB_SHAPES = listOf(
  MaterialShapes.Cookie9Sided,
  MaterialShapes.Clover8Leaf,
  MaterialShapes.Sunny,
  MaterialShapes.Flower,
  MaterialShapes.Puffy,
  MaterialShapes.SoftBurst,
  MaterialShapes.Cookie12Sided,
)

private const val MORPH_INTERVAL_MS = 2400L

private val SATELLITES = listOf(
  Satellite(MaterialShapes.Heart, size = 26F, orbit = 1F, speed = 0.7F, phase = 0F, spin = 40F),
  Satellite(MaterialShapes.Sunny, size = 22F, orbit = 1.12F, speed = 0.7F, phase = PI.toFloat() * 0.66F, spin = -60F),
  Satellite(MaterialShapes.Pill, size = 20F, orbit = 0.92F, speed = 0.7F, phase = PI.toFloat() * 1.33F, spin = 90F),
  Satellite(MaterialShapes.Cookie4Sided, size = 16F, orbit = 1.3F, speed = -0.45F, phase = PI.toFloat() * 0.4F, spin = 70F),
)
