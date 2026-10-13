@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.core.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import voice.core.data.Bookmark
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val MAX_SEGMENTS = 120

/** [position] is where in the book, from 0 to 1. */
@Immutable
data class BookBarPin(
  val position: Float,
  val kind: Bookmark.Kind,
)

/**
 * The whole book as one segment per chapter, with the current chapter's segment standing out.
 * Bookmarks sit above it as little shapes in their kind's color. Pins that would overlap are
 * stacked in rows.
 */
@Composable
fun BookBar(
  segments: List<Float>,
  currentSegment: Int,
  progress: Float,
  modifier: Modifier = Modifier,
  pins: List<BookBarPin> = emptyList(),
  barHeight: Dp = 12.dp,
  pinSize: Dp = 14.dp,
  showPositionMarker: Boolean = false,
  onPinClick: ((index: Int) -> Unit)? = null,
) {
  val colors = MaterialTheme.colorScheme
  val pinStyles = PinStyles(
    note = pinStyle(Bookmark.Kind.Note),
    favorite = pinStyle(Bookmark.Kind.Favorite),
    quote = pinStyle(Bookmark.Kind.Quote),
    revisit = pinStyle(Bookmark.Kind.Revisit),
  )
  val placements = remember(pins, segments, pinSize, LocalDensity.current) { PinPlacements(pins, segments, pinSize) }
  val currentOnPinClick = rememberUpdatedState(onPinClick)
  val tapModifier = if (onPinClick != null && pins.isNotEmpty()) {
    Modifier.pointerInput(placements) {
      detectTapGestures { offset ->
        val placement = placements.at(this, size.width.toFloat())
        val pin = placement.pinAt(offset.x, offset.y, pinSize = pinSize.toPx(), reach = 20.dp.toPx())
        if (pin != null) currentOnPinClick.value?.invoke(pin)
      }
    }
  } else {
    Modifier
  }
  Spacer(
    modifier = modifier
      .fillMaxWidth()
      .layout { measurable, constraints ->
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val height = constraints.constrainHeight((placements.at(this, width.toFloat()).barTop + barHeight.toPx()).roundToInt())
        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
      }
      .then(tapModifier)
      .drawWithCache {
        val placement = placements.at(this, size.width)
        val barTop = placement.barTop
        val barHeightPx = barHeight.toPx()
        val pinPx = pinSize.toPx()
        val gap = SEGMENT_GAP.toPx()
        val minSegmentWidth = MIN_SEGMENT_WIDTH.toPx()
        val outlines = pins.map { pin ->
          pinStyles.of(pin).shape.createOutline(Size(pinPx, pinPx), layoutDirection, this)
        }
        val cutout = Stroke(width = PIN_CUTOUT.toPx() * 2)
        val layerPaint = Paint()
        onDrawBehind {
          translate(top = barTop) {
            drawBookBar(
              barSize = Size(size.width, barHeightPx),
              segments = segments,
              gap = gap,
              minSegmentWidth = minSegmentWidth,
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
              x = bookBarX(progress, segments, size.width, gap, minSegmentWidth)
                .coerceIn(markerRadius, size.width - markerRadius),
              y = barTop + barHeightPx / 2,
            )
            drawCircle(color = colors.surface, radius = markerRadius + 2.dp.toPx(), center = center)
            drawCircle(color = colors.primary, radius = markerRadius, center = center)
          }
          if (pins.isNotEmpty()) {
            // a layer of their own, so the gap cut around each pin only clears the pins below it
            drawContext.canvas.withSaveLayer(Rect(-pinPx, -pinPx, size.width + pinPx, barTop), layerPaint) {
              pins.forEachIndexed { index, pin ->
                translate(left = placement.centers[index] - pinPx / 2, top = placement.tops[index]) {
                  drawOutline(outlines[index], color = Color.Black, style = cutout, blendMode = BlendMode.Clear)
                  drawOutline(outlines[index], color = pinStyles.of(pin).color)
                }
              }
            }
          }
        }
      },
  )
}

/**
 * Where the pins sit on a bar of some width: the [centers] and [tops] of each pin, and where the
 * bar starts below them.
 */
internal class PinPlacement(
  val centers: List<Float>,
  val tops: List<Float>,
  val barTop: Float,
) {

  /** The pin a tap at [x], [y] means, if one is within [reach] to the side. */
  fun pinAt(
    x: Float,
    y: Float,
    pinSize: Float,
    reach: Float,
  ): Int? = centers.indices
    .filter { abs(centers[it] - x) <= reach }
    .minByOrNull { hypot(centers[it] - x, tops[it] + pinSize / 2 - y) }
}

internal fun Density.placePins(
  pins: List<BookBarPin>,
  segments: List<Float>,
  width: Float,
  pinSize: Dp,
): PinPlacement {
  val pinPx = pinSize.toPx()
  val half = pinPx / 2
  val centers = pins.map { pin ->
    bookBarX(pin.position, segments, width, SEGMENT_GAP.toPx(), MIN_SEGMENT_WIDTH.toPx())
      .coerceIn(half, (width - half).coerceAtLeast(half))
  }
  val lifts = pinLifts(centers, pinWidth = (pinSize + PIN_SPACING).toPx())
  val barTop = pinAreaHeight(lifts, pinSize).toPx()
  return PinPlacement(
    centers = centers,
    tops = lifts.map { barTop - PIN_GAP.toPx() - pinPx - it * (pinPx + PIN_SPACING.toPx()) },
    barTop = barTop,
  )
}

/** Layout, drawing and taps all need the pins' places, so they're worked out once per width. */
private class PinPlacements(
  private val pins: List<BookBarPin>,
  private val segments: List<Float>,
  private val pinSize: Dp,
) {
  private var width = Float.NaN
  private var placement: PinPlacement? = null

  fun at(
    density: Density,
    width: Float,
  ): PinPlacement {
    placement?.let { if (width == this.width) return it }
    return density.placePins(pins, segments, width, pinSize).also {
      this.width = width
      placement = it
    }
  }
}

/**
 * How many rows each pin at [xs] is lifted. A pin takes the lowest row with room for it, and once
 * every row is taken, the one where it overlaps the least.
 */
internal fun pinLifts(
  xs: List<Float>,
  pinWidth: Float,
): List<Int> {
  val lifts = IntArray(xs.size)
  val rowEnds = FloatArray(PIN_ROWS) { Float.NEGATIVE_INFINITY }
  xs.indices.sortedBy { xs[it] }.forEach { index ->
    val x = xs[index]
    val row = rowEnds.indices.firstOrNull { x - rowEnds[it] >= pinWidth }
      ?: rowEnds.indices.minBy { rowEnds[it] }
    lifts[index] = row
    rowEnds[row] = x
  }
  return lifts.toList()
}

private fun pinAreaHeight(
  lifts: List<Int>,
  pinSize: Dp,
): Dp {
  val maxLift = lifts.maxOrNull() ?: return 0.dp
  return pinSize + PIN_GAP + (pinSize + PIN_SPACING) * maxLift
}

private const val PIN_ROWS = 2
private val PIN_GAP = 3.dp
private val PIN_SPACING = 2.dp
private val PIN_CUTOUT = 1.5.dp
private val SEGMENT_GAP = 3.dp
private val MIN_SEGMENT_WIDTH = 3.dp

private fun isSegmented(
  segments: List<Float>,
  width: Float,
  gap: Float,
  minSegmentWidth: Float,
): Boolean {
  val available = width - gap * (segments.size - 1).coerceAtLeast(0)
  return segments.size in 2..MAX_SEGMENTS && available / segments.size >= minSegmentWidth
}

/**
 * Where [fraction] of the book is on a bar [width] wide, laid out the way [drawBookBar] lays out
 * the segments: each segment gets its share of the width the gaps leave, so a spot in a later
 * chapter is further right by the gaps before it.
 */
internal fun bookBarX(
  fraction: Float,
  segments: List<Float>,
  width: Float,
  gap: Float,
  minSegmentWidth: Float,
): Float {
  if (!isSegmented(segments, width, gap, minSegmentWidth)) {
    return (fraction * width).coerceIn(0F, width)
  }
  val available = width - gap * (segments.size - 1)
  var left = 0F
  var start = 0F
  var index = 0
  // a spot right where a segment ends belongs to the next one, like a chapter's first second
  while (index < segments.lastIndex && fraction >= start + segments[index]) {
    left += available * segments[index] + gap
    start += segments[index]
    index++
  }
  return (left + available * (fraction - start)).coerceIn(0F, width)
}

private class PinStyle(
  val shape: Shape,
  val color: Color,
)

@Composable
private fun pinStyle(kind: Bookmark.Kind): PinStyle {
  val style = bookmarkStyle(kind)
  return PinStyle(shape = style.polygon.toShape(), color = style.pin)
}

private class PinStyles(
  val note: PinStyle,
  val favorite: PinStyle,
  val quote: PinStyle,
  val revisit: PinStyle,
) {
  fun of(pin: BookBarPin): PinStyle = when (pin.kind) {
    Bookmark.Kind.Note -> note
    Bookmark.Kind.Favorite -> favorite
    Bookmark.Kind.Quote -> quote
    Bookmark.Kind.Revisit -> revisit
  }
}

private fun DrawScope.drawBookBar(
  barSize: Size,
  segments: List<Float>,
  gap: Float,
  minSegmentWidth: Float,
  currentSegment: Int,
  progress: Float,
  activeColor: Color,
  trackColor: Color,
  currentTrackColor: Color,
) {
  val thin = barSize.height / 2
  val thick = barSize.height
  if (!isSegmented(segments, barSize.width, gap, minSegmentWidth)) {
    drawSegment(barSize, 0F, barSize.width, thin, fill = progress, activeColor = activeColor, trackColor = trackColor)
    return
  }
  val available = barSize.width - gap * (segments.size - 1)
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
