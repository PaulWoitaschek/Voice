@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import voice.core.data.Bookmark
import kotlin.math.abs

private const val MAX_SEGMENTS = 120

/**
 * A bookmark drawn on the [BookBar].
 *
 * @param position where in the book, from 0 to 1.
 */
@Immutable
data class BookBarPin(
  val position: Float,
  val kind: Bookmark.Kind,
  val setBySleepTimer: Boolean,
)

/**
 * The whole book as one segment per chapter, with the current chapter's segment standing out.
 * Bookmarks sit above it as little shapes in their kind's color. Pins that would overlap are
 * stacked.
 *
 * @param popPin index of a pin that pops in, e.g. right after it was saved.
 */
@Composable
fun BookBar(
  segments: List<Float>,
  currentSegment: Int,
  progress: Float,
  pins: List<BookBarPin>,
  modifier: Modifier = Modifier,
  barHeight: Dp = 12.dp,
  pinSize: Dp = 10.dp,
  showPositionMarker: Boolean = false,
  popPin: Int? = null,
  onPinClick: ((index: Int) -> Unit)? = null,
) {
  val colors = MaterialTheme.colorScheme
  val pinStyles = PinStyles(
    note = pinStyle(Bookmark.Kind.Note, false),
    favorite = pinStyle(Bookmark.Kind.Favorite, false),
    quote = pinStyle(Bookmark.Kind.Quote, false),
    revisit = pinStyle(Bookmark.Kind.Revisit, false),
    sleep = pinStyle(Bookmark.Kind.Note, true),
  )
  val pop = remember { Animatable(1F) }
  LaunchedEffect(popPin) {
    if (popPin != null) {
      pop.snapTo(0F)
      pop.animateTo(1F, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow))
    }
  }
  val lifts = remember(pins) { pinLifts(pins.map { it.position }) }
  val maxLift = lifts.maxOrNull() ?: 0
  val pinArea = if (pins.isEmpty()) 0.dp else pinSize + pinSize * PIN_LIFT * maxLift + PIN_GAP
  val currentOnPinClick = rememberUpdatedState(onPinClick)
  val tapModifier = if (onPinClick != null && pins.isNotEmpty()) {
    Modifier.pointerInput(pins) {
      detectTapGestures { offset ->
        val nearest = pins.indices.minByOrNull { abs(pins[it].position * size.width - offset.x) }
          ?: return@detectTapGestures
        if (abs(pins[nearest].position * size.width - offset.x) <= 20.dp.toPx()) {
          currentOnPinClick.value?.invoke(nearest)
        }
      }
    }
  } else {
    Modifier
  }
  Spacer(
    modifier = modifier
      .fillMaxWidth()
      .height(pinArea + barHeight)
      .then(tapModifier)
      .drawWithCache {
        val barTop = pinArea.toPx()
        val barHeightPx = barHeight.toPx()
        val pinPx = pinSize.toPx()
        val outlines = pins.map { pin ->
          pinStyles.of(pin).shape.createOutline(Size(pinPx, pinPx), layoutDirection, this)
        }
        onDrawBehind {
          translate(top = barTop) {
            drawBookBar(
              barSize = Size(size.width, barHeightPx),
              segments = segments,
              currentSegment = currentSegment,
              progress = progress,
              activeColor = colors.primary,
              trackColor = colors.primary.copy(alpha = 0.18F),
              currentTrackColor = colors.primary.copy(alpha = 0.32F),
            )
          }
          if (showPositionMarker) {
            val markerRadius = barHeightPx * 0.75F
            val center = Offset(
              x = (progress * size.width).coerceIn(markerRadius, size.width - markerRadius),
              y = barTop + barHeightPx / 2,
            )
            drawCircle(color = colors.surface, radius = markerRadius + 2.dp.toPx(), center = center)
            drawCircle(color = colors.primary, radius = markerRadius, center = center)
          }
          pins.forEachIndexed { index, pin ->
            val scale = if (index == popPin) pop.value else 1F
            val left = (pin.position * size.width - pinPx / 2).coerceIn(0F, size.width - pinPx)
            val top = barTop - PIN_GAP.toPx() - pinPx - lifts[index] * pinPx * PIN_LIFT
            withTransform(
              {
                translate(left = left, top = top)
                scale(scale, scale, pivot = Offset(pinPx / 2, pinPx))
              },
            ) {
              drawOutline(outlines[index], color = pinStyles.of(pin).color)
            }
          }
        }
      },
  )
}

/**
 * How many steps each pin is lifted so that pins close to each other don't overlap.
 */
internal fun pinLifts(positions: List<Float>): List<Int> {
  val lifts = IntArray(positions.size)
  var previous = Float.NEGATIVE_INFINITY
  var lift = 0
  positions.indices.sortedBy { positions[it] }.forEach { index ->
    val position = positions[index]
    lift = if (position - previous < PIN_OVERLAP) (lift + 1).coerceAtMost(MAX_LIFT) else 0
    lifts[index] = lift
    previous = position
  }
  return lifts.toList()
}

private const val PIN_OVERLAP = 0.02F
private const val PIN_LIFT = 0.6F
private const val MAX_LIFT = 2
private val PIN_GAP = 3.dp

private class PinStyle(
  val shape: Shape,
  val color: Color,
)

@Composable
private fun pinStyle(
  kind: Bookmark.Kind,
  setBySleepTimer: Boolean,
): PinStyle {
  val style = bookmarkStyle(kind, setBySleepTimer)
  return PinStyle(shape = style.polygon.toShape(), color = style.pin)
}

private class PinStyles(
  val note: PinStyle,
  val favorite: PinStyle,
  val quote: PinStyle,
  val revisit: PinStyle,
  val sleep: PinStyle,
) {
  fun of(pin: BookBarPin): PinStyle = if (pin.setBySleepTimer) {
    sleep
  } else {
    when (pin.kind) {
      Bookmark.Kind.Note -> note
      Bookmark.Kind.Favorite -> favorite
      Bookmark.Kind.Quote -> quote
      Bookmark.Kind.Revisit -> revisit
    }
  }
}

private fun DrawScope.drawBookBar(
  barSize: Size,
  segments: List<Float>,
  currentSegment: Int,
  progress: Float,
  activeColor: Color,
  trackColor: Color,
  currentTrackColor: Color,
) {
  val gap = 3.dp.toPx()
  val thin = barSize.height / 2
  val thick = barSize.height
  val totalGap = gap * (segments.size - 1).coerceAtLeast(0)
  val available = barSize.width - totalGap
  val segmented = segments.size in 2..MAX_SEGMENTS && available / segments.size >= 3.dp.toPx()
  if (!segmented) {
    drawSegment(barSize, 0F, barSize.width, thin, fill = progress, activeColor = activeColor, trackColor = trackColor)
    return
  }
  var x = 0F
  var start = 0F
  segments.forEachIndexed { index, segment ->
    val width = available * segment
    val isCurrent = index == currentSegment
    val fill = if (segment > 0F) ((progress - start) / segment).coerceIn(0F, 1F) else 0F
    drawSegment(
      barSize = barSize,
      left = x,
      width = width,
      height = if (isCurrent) thick else thin,
      fill = fill,
      activeColor = activeColor,
      trackColor = if (isCurrent) currentTrackColor else trackColor,
    )
    x += width + gap
    start += segment
  }
}

private fun DrawScope.drawSegment(
  barSize: Size,
  left: Float,
  width: Float,
  height: Float,
  fill: Float,
  activeColor: Color,
  trackColor: Color,
) {
  val top = (barSize.height - height) / 2
  val corner = CornerRadius(height / 2)
  drawRoundRect(color = trackColor, topLeft = Offset(left, top), size = Size(width, height), cornerRadius = corner)
  if (fill > 0F) {
    clipRect(left = left, top = top, right = left + width * fill, bottom = top + height) {
      drawRoundRect(color = activeColor, topLeft = Offset(left, top), size = Size(width, height), cornerRadius = corner)
    }
  }
}
