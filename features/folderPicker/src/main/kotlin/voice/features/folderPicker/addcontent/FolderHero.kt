@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.folderPicker.addcontent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.sin
import androidx.graphics.shapes.toPath as toAndroidPath

/**
 * A folder full of audio files bobbing about. Tapping it (just for fun, so it's hidden from
 * accessibility services) makes the flap jump open and the files hop out and back in.
 */
@Composable
internal fun FolderHero(
  clock: () -> Float,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val colors = MaterialTheme.colorScheme
  val open = remember { Animatable(0F) }
  val hops = remember { FILES.map { Animatable(0F) } }
  val badgePath = remember { MaterialShapes.Sunny.toAndroidPath().asComposePath() }
  LaunchedEffect(open, hops) {
    // says hello once the screen has floated in
    delay(500)
    toss(open, hops)
  }
  Canvas(
    modifier = modifier
      .clearAndSetSemantics {}
      .pointerInput(Unit) {
        detectTapGestures {
          haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
          scope.launch { toss(open, hops) }
        }
      },
  ) {
    val t = clock()
    val width = min(min(size.width * 0.62F, size.height * 0.95F), 300.dp.toPx())
    val height = width * 0.78F
    val left = center.x - width / 2
    // the files sticking out of the top count towards the height, so this shifts the folder down a bit
    val top = center.y - height / 2 + width * 0.08F
    val corner = CornerRadius(width * 0.08F)

    drawRoundRect(
      color = colors.secondary,
      topLeft = Offset(left + width * 0.06F, top),
      size = Size(width * 0.36F, height * 0.3F),
      cornerRadius = corner,
    )
    drawRoundRect(
      color = colors.secondary,
      topLeft = Offset(left, top + height * 0.12F),
      size = Size(width, height * 0.88F),
      cornerRadius = corner,
    )

    FILES.forEachIndexed { index, file ->
      val (paper, ink) = file.colors(colors)
      val bob = sin(t * 2.2F + index * 1.9F) * width * 0.025F
      val hop = hops[index].value
      val cardWidth = width * 0.27F
      val cardHeight = width * 0.36F
      val cardLeft = left + width * file.x - cardWidth / 2
      val cardTop = top + height * 0.02F + bob - hop * width * 0.34F
      rotate(
        degrees = file.tilt + hop * file.tilt * 1.6F,
        pivot = Offset(cardLeft + cardWidth / 2, cardTop + cardHeight),
      ) {
        drawFile(t = t + index, left = cardLeft, top = cardTop, width = cardWidth, height = cardHeight, paper = paper, ink = ink)
      }
    }

    // the flap tips forward when opening, which looks like it shrinks towards its bottom edge
    val flapTop = top + height * 0.36F
    scale(scaleX = 1F + open.value * 0.04F, scaleY = 1F - open.value * 0.3F, pivot = Offset(center.x, top + height)) {
      drawRoundRect(
        color = colors.primary,
        topLeft = Offset(left, flapTop),
        size = Size(width, top + height - flapTop),
        cornerRadius = corner,
      )
      drawRoundRect(
        color = colors.onPrimary,
        alpha = 0.35F,
        topLeft = Offset(center.x - width * 0.16F, flapTop + (top + height - flapTop) * 0.42F),
        size = Size(width * 0.32F, width * 0.06F),
        cornerRadius = CornerRadius(width * 0.03F),
      )
    }

    // a little plus badge, saying "add me"
    val badgeSize = width * 0.28F * (1F + open.value * 0.25F)
    val badgeCenter = Offset(left + width * 0.92F, flapTop + width * 0.02F)
    translate(left = badgeCenter.x - badgeSize / 2, top = badgeCenter.y - badgeSize / 2) {
      rotate(degrees = t * 24F, pivot = Offset(badgeSize / 2, badgeSize / 2)) {
        scale(scaleX = badgeSize, scaleY = badgeSize, pivot = Offset.Zero) {
          drawPath(path = badgePath, color = colors.tertiary)
        }
      }
    }
    val armLength = badgeSize * 0.42F
    val armWidth = badgeSize * 0.11F
    drawRoundRect(
      color = colors.onTertiary,
      topLeft = Offset(badgeCenter.x - armLength / 2, badgeCenter.y - armWidth / 2),
      size = Size(armLength, armWidth),
      cornerRadius = CornerRadius(armWidth / 2),
    )
    drawRoundRect(
      color = colors.onTertiary,
      topLeft = Offset(badgeCenter.x - armWidth / 2, badgeCenter.y - armLength / 2),
      size = Size(armWidth, armLength),
      cornerRadius = CornerRadius(armWidth / 2),
    )
  }
}

private suspend fun toss(
  open: Animatable<Float, *>,
  hops: List<Animatable<Float, *>>,
) = coroutineScope {
  launch {
    open.animateTo(1F, tween(durationMillis = 160, easing = FastOutSlowInEasing))
    open.animateTo(0F, spring(dampingRatio = 0.3F, stiffness = Spring.StiffnessLow))
  }
  hops.forEachIndexed { index, hop ->
    launch {
      delay(60L + index * 70L)
      hop.animateTo(1F, tween(durationMillis = 200, easing = FastOutSlowInEasing))
      hop.animateTo(0F, spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessMediumLow))
    }
  }
}

/** A sheet of paper with a little waveform on it. */
private fun DrawScope.drawFile(
  t: Float,
  left: Float,
  top: Float,
  width: Float,
  height: Float,
  paper: Color,
  ink: Color,
) {
  drawRoundRect(
    color = paper,
    topLeft = Offset(left, top),
    size = Size(width, height),
    cornerRadius = CornerRadius(width * 0.18F),
  )
  val barCount = 4
  val barWidth = width * 0.1F
  val gap = width * 0.08F
  val totalWidth = barCount * barWidth + (barCount - 1) * gap
  repeat(barCount) { bar ->
    val level = 0.55F + 0.45F * sin(t * (4.2F + bar * 0.8F) + bar * 1.7F)
    val barHeight = height * (0.12F + 0.22F * level)
    drawRoundRect(
      color = ink,
      topLeft = Offset(left + (width - totalWidth) / 2 + bar * (barWidth + gap), top + height * 0.36F - barHeight / 2),
      size = Size(barWidth, barHeight),
      cornerRadius = CornerRadius(barWidth / 2),
    )
  }
}

private enum class FileColor {
  Primary,
  Secondary,
  Tertiary,
}

private class AudioFile(
  val x: Float,
  val tilt: Float,
  val color: FileColor,
) {
  fun colors(scheme: ColorScheme): Pair<Color, Color> = when (color) {
    FileColor.Primary -> scheme.primaryContainer to scheme.onPrimaryContainer
    FileColor.Secondary -> scheme.secondaryContainer to scheme.onSecondaryContainer
    FileColor.Tertiary -> scheme.tertiaryContainer to scheme.onTertiaryContainer
  }
}

private val FILES = listOf(
  AudioFile(x = 0.27F, tilt = -10F, color = FileColor.Tertiary),
  AudioFile(x = 0.5F, tilt = 2F, color = FileColor.Primary),
  AudioFile(x = 0.73F, tilt = 12F, color = FileColor.Secondary),
)
