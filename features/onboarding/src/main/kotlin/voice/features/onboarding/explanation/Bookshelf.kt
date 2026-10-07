package voice.features.onboarding.explanation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * A little bookshelf filling up: books drop onto it one after another and land with a bounce. Now
 * and then one of them hops. Tapping the shelf sends a wave through all of them.
 */
@Composable
internal fun Bookshelf(
  clock: () -> Float,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val colors = MaterialTheme.colorScheme
  // 1 is still up in the air, 0 is standing on the shelf
  val drops = remember { BOOKS.map { Animatable(1F) } }
  val hops = remember { BOOKS.map { Animatable(0F) } }
  LaunchedEffect(drops) {
    drops.forEachIndexed { index, drop ->
      launch {
        delay(200L + index * 110L)
        drop.animateTo(0F, spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessLow))
      }
    }
  }
  LaunchedEffect(hops) {
    if (coroutineContext[MotionDurationScale]?.scaleFactor == 0F) return@LaunchedEffect
    val random = Random(seed = 7)
    delay(2200)
    while (true) {
      hop(hops[random.nextInt(hops.size)], height = 0.6F)
      delay(1800L + random.nextLong(1600L))
    }
  }
  val sparkle = remember { sparklePath() }
  Canvas(
    modifier = modifier
      .clearAndSetSemantics {}
      .pointerInput(Unit) {
        detectTapGestures {
          haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
          hops.forEachIndexed { index, hop ->
            scope.launch {
              delay(index * 55L)
              hop(hop, height = 1F)
            }
          }
        }
      },
  ) {
    val t = clock()
    val shelfWidth = min(min(size.width * 0.8F, size.height * 1.3F), 340.dp.toPx())
    val maxBookHeight = min(size.height * 0.56F, shelfWidth * 0.62F)
    val shelfThickness = 12.dp.toPx()
    val shelfTop = center.y + maxBookHeight / 2F
    val shelfLeft = center.x - shelfWidth / 2F

    // sparkles twinkling above the shelf
    SPARKLES.forEach { spark ->
      val twinkle = 0.5F + 0.5F * sin(t * spark.speed + spark.phase)
      val sparkSize = spark.size.dp.toPx() * (0.6F + 0.4F * twinkle)
      translate(
        left = shelfLeft + shelfWidth * spark.x - sparkSize / 2,
        top = shelfTop - maxBookHeight * spark.y - sparkSize / 2,
      ) {
        scale(scaleX = sparkSize, scaleY = sparkSize, pivot = Offset.Zero) {
          drawPath(path = sparkle, color = colors.tertiary, alpha = 0.3F + 0.7F * twinkle)
        }
      }
    }

    val gap = 5.dp.toPx()
    val leanGap = maxBookHeight * 0.18F
    val naturalWidth = BOOKS.sumOf { it.width.toDouble() }.toFloat()
    val booksWidth = shelfWidth * 0.86F - gap * (BOOKS.size - 1) - leanGap
    val unit = booksWidth / naturalWidth
    var x = center.x - shelfWidth * 0.86F / 2F
    BOOKS.forEachIndexed { index, book ->
      val bookWidth = book.width * unit
      val bookHeight = book.height * maxBookHeight
      if (book.leaning) x += leanGap
      val (cover, accent) = book.colors(colors)
      // the springs overshoot below zero. Mirroring that makes the books bounce off the shelf instead
      // of sinking into it.
      val drop = abs(drops[index].value)
      val lift = drop * (shelfTop + bookHeight) + abs(hops[index].value) * 22.dp.toPx()
      // tumbling a bit while falling, alternating directions
      val wobble = drop * if (index % 2 == 0) 18F else -18F
      val tilt = if (book.leaning) -16F * (1F - drop) else 0F
      val left = x
      val top = shelfTop - bookHeight - lift
      rotate(
        degrees = wobble + tilt,
        // the leaning book tips over its bottom right corner onto its neighbor
        pivot = if (book.leaning) Offset(left + bookWidth, shelfTop - lift) else Offset(left + bookWidth / 2, top + bookHeight / 2),
      ) {
        drawBook(left = left, top = top, width = bookWidth, height = bookHeight, cover = cover, accent = accent, band = book.band)
      }
      x += bookWidth + gap
    }

    drawRoundRect(
      color = colors.secondary,
      topLeft = Offset(shelfLeft, shelfTop),
      size = Size(shelfWidth, shelfThickness),
      cornerRadius = CornerRadius(shelfThickness / 2),
    )
  }
}

private suspend fun hop(
  hop: Animatable<Float, *>,
  height: Float,
) {
  hop.animateTo(height, tween(durationMillis = 140, easing = FastOutSlowInEasing))
  hop.animateTo(0F, spring(dampingRatio = 0.35F, stiffness = Spring.StiffnessMediumLow))
}

private fun DrawScope.drawBook(
  left: Float,
  top: Float,
  width: Float,
  height: Float,
  cover: Color,
  accent: Color,
  band: Float,
) {
  val corner = CornerRadius(min(width * 0.28F, 8.dp.toPx()))
  drawRoundRect(color = cover, topLeft = Offset(left, top), size = Size(width, height), cornerRadius = corner)
  val inset = width * 0.2F
  val bandHeight = width * 0.22F
  drawRoundRect(
    color = accent,
    topLeft = Offset(left + inset, top + height * band),
    size = Size(width - inset * 2, bandHeight),
    cornerRadius = CornerRadius(bandHeight / 2),
  )
  drawRoundRect(
    color = accent,
    topLeft = Offset(left + inset, top + height * band + bandHeight * 1.8F),
    size = Size(width - inset * 2, bandHeight * 0.6F),
    cornerRadius = CornerRadius(bandHeight / 2),
    alpha = 0.6F,
  )
  drawCircle(
    color = accent,
    radius = width * 0.14F,
    center = Offset(left + width / 2, top + height - width * 0.4F),
  )
}

private enum class BookColor {
  Primary,
  PrimaryContainer,
  Secondary,
  SecondaryContainer,
  Tertiary,
  TertiaryContainer,
}

private class Book(
  val width: Float,
  val height: Float,
  val color: BookColor,
  val band: Float,
  val leaning: Boolean = false,
) {
  fun colors(scheme: ColorScheme): Pair<Color, Color> = when (color) {
    BookColor.Primary -> scheme.primary to scheme.onPrimary.copy(alpha = 0.55F)
    BookColor.PrimaryContainer -> scheme.primaryContainer to scheme.onPrimaryContainer.copy(alpha = 0.4F)
    BookColor.Secondary -> scheme.secondary to scheme.onSecondary.copy(alpha = 0.55F)
    BookColor.SecondaryContainer -> scheme.secondaryContainer to scheme.onSecondaryContainer.copy(alpha = 0.4F)
    BookColor.Tertiary -> scheme.tertiary to scheme.onTertiary.copy(alpha = 0.55F)
    BookColor.TertiaryContainer -> scheme.tertiaryContainer to scheme.onTertiaryContainer.copy(alpha = 0.4F)
  }
}

private val BOOKS = listOf(
  Book(width = 26F, height = 0.78F, color = BookColor.Primary, band = 0.16F),
  Book(width = 34F, height = 0.94F, color = BookColor.TertiaryContainer, band = 0.22F),
  Book(width = 22F, height = 0.66F, color = BookColor.Secondary, band = 0.14F),
  Book(width = 30F, height = 0.86F, color = BookColor.PrimaryContainer, band = 0.2F),
  Book(width = 26F, height = 0.72F, color = BookColor.Tertiary, band = 0.18F),
  Book(width = 36F, height = 1F, color = BookColor.SecondaryContainer, band = 0.26F),
  Book(width = 24F, height = 0.7F, color = BookColor.Primary, band = 0.16F, leaning = true),
)

private class Sparkle(
  val x: Float,
  val y: Float,
  val size: Float,
  val speed: Float,
  val phase: Float,
)

private val SPARKLES = listOf(
  Sparkle(x = 0.08F, y = 1.22F, size = 18F, speed = 2.1F, phase = 0F),
  Sparkle(x = 0.9F, y = 1.36F, size = 24F, speed = 1.6F, phase = 1.7F),
  Sparkle(x = 0.62F, y = 1.5F, size = 12F, speed = 2.8F, phase = 3.1F),
)

/** A four pointed sparkle in a 1x1 box. */
private fun sparklePath(): Path = Path().apply {
  moveTo(0.5F, 0F)
  quadraticTo(0.5F, 0.5F, 1F, 0.5F)
  quadraticTo(0.5F, 0.5F, 0.5F, 1F)
  quadraticTo(0.5F, 0.5F, 0F, 0.5F)
  quadraticTo(0.5F, 0.5F, 0.5F, 0F)
  close()
}
