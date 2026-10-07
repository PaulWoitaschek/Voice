@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.onboarding.completion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import voice.core.ui.drawConfetti
import voice.core.ui.rememberConfettiState
import kotlin.math.min
import kotlin.math.sin
import androidx.graphics.shapes.toPath as toAndroidPath

/**
 * A badge that pops in, ticks itself off and throws confetti made of Material shapes. Tapping it
 * (just for fun, so it's hidden from accessibility services) throws another handful.
 */
@Composable
internal fun Celebration(
  clock: () -> Float,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val colors = MaterialTheme.colorScheme
  val confettiColors = listOf(
    colors.primary,
    colors.secondary,
    colors.tertiary,
    colors.primaryContainer,
    colors.secondaryContainer,
    colors.tertiaryContainer,
  )
  val badgePath = remember { MaterialShapes.SoftBurst.toAndroidPath().asComposePath() }
  val haloPath = remember { MaterialShapes.Cookie12Sided.toAndroidPath().asComposePath() }
  val checkMeasure = remember { PathMeasure().apply { setPath(checkPath(), forceClosed = false) } }
  val checkSegment = remember { Path() }
  val confetti = rememberConfettiState()
  val pop = remember { Animatable(0F) }
  val check = remember { Animatable(0F) }
  var pressed by remember { mutableStateOf(false) }
  val squish by animateFloatAsState(
    targetValue = if (pressed) 0.86F else 1F,
    animationSpec = spring(dampingRatio = 0.35F, stiffness = Spring.StiffnessMedium),
    label = "badgeSquish",
  )
  LaunchedEffect(pop, check) {
    delay(200)
    launch { pop.animateTo(1F, spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessLow)) }
    delay(150)
    confetti.burst()
    delay(150)
    check.animateTo(1F, tween(durationMillis = 450, easing = FastOutSlowInEasing))
  }
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
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            confetti.burst()
          },
        )
      },
  ) {
    val t = clock()
    val radius = min(min(size.width, size.height) * 0.3F, 120.dp.toPx())

    val haloSize = radius * 2.5F * pop.value
    translate(left = center.x - haloSize / 2, top = center.y - haloSize / 2) {
      rotate(degrees = -t * 6F, pivot = Offset(haloSize / 2, haloSize / 2)) {
        scale(scaleX = haloSize, scaleY = haloSize, pivot = Offset.Zero) {
          drawPath(path = haloPath, color = colors.tertiaryContainer, alpha = 0.7F)
        }
      }
    }

    val badgeSize = radius * 2F * pop.value * squish * (1F + 0.025F * sin(t * 2.4F))
    translate(left = center.x - badgeSize / 2, top = center.y - badgeSize / 2) {
      // spins in while popping up, then keeps turning slowly
      rotate(degrees = t * 10F - (1F - pop.value) * 120F, pivot = Offset(badgeSize / 2, badgeSize / 2)) {
        scale(scaleX = badgeSize, scaleY = badgeSize, pivot = Offset.Zero) {
          drawPath(path = badgePath, color = colors.primary)
        }
      }
      checkSegment.reset()
      checkMeasure.getSegment(0F, checkMeasure.length * check.value, checkSegment, startWithMoveTo = true)
      scale(scaleX = badgeSize, scaleY = badgeSize, pivot = Offset.Zero) {
        drawPath(
          path = checkSegment,
          color = colors.onPrimary,
          style = Stroke(width = 0.09F, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
      }
    }

    drawConfetti(state = confetti, center = center, radius = radius, colors = confettiColors)
  }
}

/** A check mark in a 1x1 box. */
private fun checkPath(): Path = Path().apply {
  moveTo(0.3F, 0.52F)
  lineTo(0.44F, 0.66F)
  lineTo(0.71F, 0.38F)
}
