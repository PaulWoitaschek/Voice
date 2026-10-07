package voice.features.playbackScreen.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Soft blobs of the cover-derived container colors drifting around while audio plays.
 * When a sleep timer is running, a field of twinkling sparkles fades in.
 */
@Composable
internal fun AuroraBackground(
  clock: () -> Float,
  showStars: Boolean,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val starAlpha by animateFloatAsState(
    targetValue = if (showStars) 1F else 0F,
    animationSpec = tween(durationMillis = 1500),
    label = "starAlpha",
  )
  val stars = remember { generateStars() }
  // Dark containers can still be quite bright (yellows especially), so keep the blobs subtle there
  // to preserve text contrast against the surface.
  val strength = if (colorScheme.surface.luminance() < 0.5F) 0.35F else 0.85F
  Spacer(
    modifier = modifier.drawWithCache {
      val sparkle = sparklePath()
      // built once and moved around per frame, as every new gradient allocates a shader
      val primaryBlob = blobBrush(colorScheme.primaryContainer.copy(alpha = strength))
      val tertiaryBlob = blobBrush(colorScheme.tertiaryContainer.copy(alpha = strength * 0.8F))
      val secondaryBlob = blobBrush(colorScheme.secondaryContainer.copy(alpha = strength * 0.85F))
      onDrawBehind {
        val t = clock()
        val w = size.width
        val h = size.height
        val radius = max(w, h) * 0.7F
        drawRect(colorScheme.surface)
        drawBlob(
          brush = primaryBlob,
          center = Offset(w * (0.25F + 0.2F * sin(t * 0.21F)), h * (0.18F + 0.1F * cos(t * 0.17F))),
          radius = radius * (1F + 0.08F * sin(t * 0.5F)),
        )
        drawBlob(
          brush = tertiaryBlob,
          center = Offset(w * (0.85F + 0.15F * cos(t * 0.19F + 1F)), h * (0.5F + 0.12F * sin(t * 0.23F))),
          radius = radius * 0.85F,
        )
        drawBlob(
          brush = secondaryBlob,
          center = Offset(w * (0.2F + 0.25F * sin(t * 0.13F + 2F)), h * (0.95F + 0.06F * cos(t * 0.29F))),
          radius = radius * (1F + 0.1F * cos(t * 0.4F)),
        )
        if (starAlpha > 0F) {
          stars.forEach { star ->
            val twinkle = 0.25F + 0.75F * (0.5F + 0.5F * sin(t * star.speed + star.phase))
            val starSize = star.size.dp.toPx()
            withTransform(
              {
                translate(left = star.x * w, top = star.y * h)
                scale(scaleX = starSize, scaleY = starSize, pivot = Offset.Zero)
              },
            ) {
              drawPath(path = sparkle, color = colorScheme.primary, alpha = starAlpha * twinkle)
            }
          }
        }
      }
    },
  )
}

/** A soft blob with a radius of 1, centered at the origin. */
private fun blobBrush(color: Color): Brush = Brush.radialGradient(
  0F to color,
  1F to color.copy(alpha = 0F),
  center = Offset.Zero,
  radius = 1F,
)

private fun DrawScope.drawBlob(
  brush: Brush,
  center: Offset,
  radius: Float,
) {
  withTransform(
    {
      translate(left = center.x, top = center.y)
      scale(scaleX = radius, scaleY = radius, pivot = Offset.Zero)
    },
  ) {
    drawCircle(brush = brush, radius = 1F, center = Offset.Zero)
  }
}

private class Star(
  val x: Float,
  val y: Float,
  val size: Float,
  val phase: Float,
  val speed: Float,
)

private fun generateStars(): List<Star> {
  val random = Random(seed = 42)
  return List(36) {
    Star(
      x = random.nextFloat(),
      y = random.nextFloat() * 0.6F,
      size = 2F + random.nextFloat() * 4F,
      phase = random.nextFloat() * 2 * PI.toFloat(),
      speed = 1.2F + random.nextFloat() * 2.4F,
    )
  }
}

/** A four pointed sparkle with a radius of 1, centered at the origin. */
private fun sparklePath(): Path = Path().apply {
  moveTo(0F, -1F)
  quadraticTo(0F, 0F, 1F, 0F)
  quadraticTo(0F, 0F, 0F, 1F)
  quadraticTo(0F, 0F, -1F, 0F)
  quadraticTo(0F, 0F, 0F, -1F)
  close()
}
