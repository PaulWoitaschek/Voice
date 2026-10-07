@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.core.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlinx.coroutines.channels.Channel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.graphics.shapes.toPath as toAndroidPath

/**
 * Confetti made of Material shapes. Every [burst] tosses a handful out of a ring around the center,
 * which then floats down like paper and fades out. Draw it with [drawConfetti].
 */
@Stable
class ConfettiState internal constructor() {

  internal val paths = CONFETTI_SHAPES.map { it.toAndroidPath().asComposePath() }
  private val flying = mutableListOf<Particle>()

  // the same list after every step, which still makes everything drawing it redraw
  private val state = mutableStateOf<List<Particle>>(flying, neverEqualPolicy())
  internal val particles: List<Particle> by state
  private val bursts = Channel<Unit>(Channel.CONFLATED)
  private val random = Random.Default

  fun burst() {
    bursts.trySend(Unit)
  }

  internal suspend fun run() {
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
        shape = random.nextInt(CONFETTI_SHAPES.size),
        color = random.nextInt(Int.MAX_VALUE),
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

/** Confetti that flies while composed. Confetti is pure motion, so with animations turned off, there's none. */
@Composable
fun rememberConfettiState(): ConfettiState {
  val state = remember { ConfettiState() }
  LaunchedEffect(state) {
    if (coroutineContext[MotionDurationScale]?.scaleFactor == 0F) return@LaunchedEffect
    state.run()
  }
  return state
}

/**
 * Draws the flying confetti of [state] around [center]. Positions, speeds and sizes are in multiples
 * of [radius], so the physics don't depend on the screen size. Each piece picks one of the [colors].
 */
fun DrawScope.drawConfetti(
  state: ConfettiState,
  center: Offset,
  radius: Float,
  colors: List<Color>,
) {
  state.particles.forEach { particle ->
    val sizePx = particle.size * radius
    val fade = ((particle.lifetime - particle.age) / 0.5F).coerceIn(0F, 1F)
    translate(left = center.x + particle.x * radius, top = center.y + particle.y * radius) {
      rotate(degrees = particle.rotation, pivot = Offset.Zero) {
        // squashing it horizontally back and forth looks like it's flipping over in the air
        scale(scaleX = sizePx * cos(particle.age * particle.flip), scaleY = sizePx, pivot = Offset.Zero) {
          translate(left = -0.5F, top = -0.5F) {
            drawPath(
              path = state.paths[particle.shape],
              color = colors[particle.color % colors.size],
              alpha = fade,
            )
          }
        }
      }
    }
  }
}

internal class Particle(
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
