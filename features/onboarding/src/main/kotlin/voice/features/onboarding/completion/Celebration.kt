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
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
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
  val confettiPaths = remember { CONFETTI_SHAPES.map { it.toAndroidPath().asComposePath() } }
  val checkMeasure = remember { PathMeasure().apply { setPath(checkPath(), forceClosed = false) } }
  val checkSegment = remember { Path() }
  val confetti = remember { Confetti(colorCount = 6, shapeCount = CONFETTI_SHAPES.size) }
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
  LaunchedEffect(confetti) {
    // confetti is pure motion, so without animations there's none
    if (coroutineContext[MotionDurationScale]?.scaleFactor == 0F) return@LaunchedEffect
    confetti.run()
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

    confetti.particles.forEach { particle ->
      val sizePx = particle.size * radius
      val fade = ((particle.lifetime - particle.age) / 0.5F).coerceIn(0F, 1F)
      translate(left = center.x + particle.x * radius, top = center.y + particle.y * radius) {
        rotate(degrees = particle.rotation, pivot = Offset.Zero) {
          // squashing it horizontally back and forth looks like it's flipping over in the air
          scale(scaleX = sizePx * cos(particle.age * particle.flip), scaleY = sizePx, pivot = Offset.Zero) {
            translate(left = -0.5F, top = -0.5F) {
              drawPath(path = confettiPaths[particle.shape], color = confettiColors[particle.color], alpha = fade)
            }
          }
        }
      }
    }
  }
}

/** A check mark in a 1x1 box. */
private fun checkPath(): Path = Path().apply {
  moveTo(0.3F, 0.52F)
  lineTo(0.44F, 0.66F)
  lineTo(0.71F, 0.38F)
}

/** Positions, speeds and sizes are in badge radii, so the physics don't depend on the screen size. */
private class Particle(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  var rotation: Float,
  val spin: Float,
  val flip: Float,
  val size: Float,
  val shape: Int,
  val color: Int,
  val lifetime: Float,
) {
  var age = 0F
}

private class Confetti(
  private val colorCount: Int,
  private val shapeCount: Int,
) {

  private val flying = mutableListOf<Particle>()

  // the same list after every step, which still makes everything drawing it redraw
  private val state = mutableStateOf<List<Particle>>(flying, neverEqualPolicy())
  val particles: List<Particle> by state
  private val bursts = Channel<Unit>(Channel.CONFLATED)
  private val random = Random.Default

  fun burst() {
    bursts.trySend(Unit)
  }

  suspend fun run() {
    var lastFrame = Long.MIN_VALUE
    while (true) {
      if (flying.isEmpty()) {
        bursts.receive()
        spawn()
        lastFrame = Long.MIN_VALUE
      } else if (bursts.tryReceive().isSuccess) {
        spawn()
      }
      withFrameNanos { now ->
        val seconds = if (lastFrame == Long.MIN_VALUE) 0F else ((now - lastFrame) / 1E9F).coerceAtMost(0.05F)
        lastFrame = now
        step(seconds)
        state.value = flying
      }
    }
  }

  private fun spawn() {
    repeat(PARTICLE_COUNT) {
      val angle = random.nextFloat() * 2 * PI.toFloat()
      val speed = 2F + random.nextFloat() * 3F
      flying += Particle(
        x = cos(angle) * 0.8F,
        y = sin(angle) * 0.8F,
        vx = cos(angle) * speed,
        // a bit of an upward toss, so it rains down afterwards
        vy = sin(angle) * speed - 1.6F,
        rotation = random.nextFloat() * 360F,
        spin = (random.nextFloat() - 0.5F) * 720F,
        flip = 4F + random.nextFloat() * 8F,
        size = 0.14F + random.nextFloat() * 0.14F,
        shape = random.nextInt(shapeCount),
        color = random.nextInt(colorCount),
        lifetime = 2.4F + random.nextFloat() * 1.2F,
      )
    }
  }

  private fun step(seconds: Float) {
    // lots of air resistance, so it floats down like paper instead of dropping like stones
    val drag = 1F - 2.4F * seconds
    val iterator = flying.iterator()
    while (iterator.hasNext()) {
      val particle = iterator.next()
      particle.age += seconds
      if (particle.age >= particle.lifetime) {
        iterator.remove()
        continue
      }
      particle.vx *= drag
      particle.vy = particle.vy * drag + GRAVITY * seconds
      // swaying from side to side on the way down
      particle.x += (particle.vx + sin(particle.age * 3F + particle.flip) * 0.3F) * seconds
      particle.y += particle.vy * seconds
      particle.rotation += particle.spin * seconds
    }
  }
}

private const val PARTICLE_COUNT = 56
private const val GRAVITY = 2.6F

private val CONFETTI_SHAPES = listOf(
  MaterialShapes.Heart,
  MaterialShapes.Sunny,
  MaterialShapes.Pill,
  MaterialShapes.Cookie4Sided,
  MaterialShapes.Clover4Leaf,
  MaterialShapes.Triangle,
  MaterialShapes.Circle,
)
