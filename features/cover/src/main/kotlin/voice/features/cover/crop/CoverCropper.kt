@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.cover.crop

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImage
import voice.core.ui.ShapedIcon
import voice.core.ui.icons.VoiceIcons
import kotlin.math.max
import kotlin.math.min
import voice.core.strings.R as StringsR

/** [onCropChange] gets the frame relative to the image, from 0 to 1 on both axes. */
@Composable
internal fun CoverCropper(
  cover: Uri,
  onCropChange: (Rect) -> Unit,
  modifier: Modifier = Modifier,
) {
  var aspectRatio: Float? by remember { mutableStateOf(null) }
  var failed by remember { mutableStateOf(false) }
  Box(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center,
  ) {
    val ratio = aspectRatio
    Box(
      modifier = Modifier
        .then(
          if (ratio != null) {
            Modifier.aspectRatio(ratio)
          } else {
            Modifier
              .fillMaxWidth()
              .height(240.dp)
          },
        ),
      contentAlignment = Alignment.Center,
    ) {
      AsyncImage(
        model = cover,
        contentDescription = null,
        // the box takes the image's aspect ratio, so nothing is distorted and the frame maps onto the image
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
          .matchParentSize()
          .clip(RoundedCornerShape(IMAGE_CORNER_RADIUS))
          .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        onSuccess = { state ->
          val drawable = state.result.drawable
          if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
            aspectRatio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
          }
        },
        onError = { failed = true },
      )
      if (failed) {
        LoadingFailed()
      } else if (ratio == null) {
        LoadingIndicator()
      } else {
        CropFrame(
          onCropChange = onCropChange,
          modifier = Modifier.matchParentSize(),
        )
      }
    }
  }
}

@Composable
private fun LoadingFailed() {
  val colors = MaterialTheme.colorScheme
  Column(
    modifier = Modifier.padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    ShapedIcon(
      icon = VoiceIcons.Image,
      shape = MaterialShapes.SoftBurst,
      containerColor = colors.errorContainer,
      contentColor = colors.onErrorContainer,
    )
    Text(
      text = stringResource(StringsR.string.common_error_generic_message),
      style = MaterialTheme.typography.bodyMedium,
      color = colors.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun CropFrame(
  onCropChange: (Rect) -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentOnCropChange by rememberUpdatedState(onCropChange)
  var bounds by remember { mutableStateOf(IntSize.Zero) }
  var crop by remember { mutableStateOf(Rect.Zero) }
  var active by remember { mutableStateOf(false) }
  val activeProgress by animateFloatAsState(
    targetValue = if (active) 1F else 0F,
    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
    label = "cropActive",
  )
  Box(
    modifier = modifier
      .onSizeChanged { size ->
        if (size != bounds) {
          bounds = size
          val side = min(size.width, size.height).toFloat()
          crop = Rect(
            offset = Offset((size.width - side) / 2F, (size.height - side) / 2F),
            size = Size(side, side),
          )
          currentOnCropChange(crop.relativeTo(size))
        }
      }
      .pointerInput(Unit) {
        val grabRadius = 32.dp.toPx()
        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false)
          val area = size.toSize()
          val minSide = min(area.width, area.height) / 4F
          var corner = Corner.entries
            .map { it to (crop.point(it) - down.position).getDistance() }
            .filter { (_, distance) -> distance <= grabRadius }
            .minByOrNull { (_, distance) -> distance }
            ?.first
          var cornerPosition = corner?.let(crop::point) ?: Offset.Zero
          active = true
          do {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }
            crop = when {
              pressed.size > 1 -> {
                corner = null
                crop.zoomed(event.calculateZoom(), minSide, area).moved(event.calculatePan(), area)
              }
              corner != null -> {
                cornerPosition += pressed.firstOrNull()?.positionChange() ?: Offset.Zero
                crop.resized(corner, cornerPosition, minSide, area)
              }
              else -> crop.moved(event.calculatePan(), area)
            }
            event.changes.forEach { if (it.positionChanged()) it.consume() }
          } while (event.changes.any { it.pressed })
          active = false
          currentOnCropChange(crop.relativeTo(size))
        }
      }
      .drawBehind {
        if (crop.isEmpty) return@drawBehind
        val image = Path().apply {
          addRoundRect(RoundRect(size.toRect(), CornerRadius(IMAGE_CORNER_RADIUS.toPx())))
        }
        val frame = Path().apply {
          addRoundRect(RoundRect(crop, CornerRadius(crop.width * FRAME_CORNER_RADIUS)))
        }
        // the frame itself is drawn unclipped, so it stays whole when it reaches the edges of the image
        clipPath(image) {
          clipPath(frame, ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.55F))
          }
          if (activeProgress > 0F) {
            clipPath(frame) {
              drawThirds(crop, Color.White.copy(alpha = 0.6F * activeProgress))
            }
          }
        }
        drawPath(frame, Color.White, style = Stroke(width = 1.5.dp.toPx()))
        drawCornerBrackets(crop, strokeWidth = (3F + 2F * activeProgress).dp.toPx())
      },
  )
}

private const val FRAME_CORNER_RADIUS = 0.12F
private val IMAGE_CORNER_RADIUS = 16.dp

private enum class Corner(
  val horizontal: Int,
  val vertical: Int,
) {
  TopLeft(-1, -1),
  TopRight(1, -1),
  BottomRight(1, 1),
  BottomLeft(-1, 1),
}

private fun Rect.point(corner: Corner): Offset = Offset(
  x = if (corner.horizontal < 0) left else right,
  y = if (corner.vertical < 0) top else bottom,
)

private fun Rect.relativeTo(size: IntSize): Rect = Rect(
  left = left / size.width,
  top = top / size.height,
  right = right / size.width,
  bottom = bottom / size.height,
)

private fun Rect.moved(
  by: Offset,
  area: Size,
): Rect = Rect(
  offset = Offset(
    x = (left + by.x).coerceIn(0F, max(0F, area.width - width)),
    y = (top + by.y).coerceIn(0F, max(0F, area.height - height)),
  ),
  size = size,
)

private fun Rect.zoomed(
  zoom: Float,
  minSide: Float,
  area: Size,
): Rect {
  val side = (width * zoom).coerceIn(minSide, min(area.width, area.height))
  return Rect(center = center, radius = side / 2F).moved(Offset.Zero, area)
}

private fun Rect.resized(
  corner: Corner,
  position: Offset,
  minSide: Float,
  area: Size,
): Rect {
  val anchor = point(Corner.entries.first { it.horizontal == -corner.horizontal && it.vertical == -corner.vertical })
  val maxSide = min(
    if (corner.horizontal < 0) anchor.x else area.width - anchor.x,
    if (corner.vertical < 0) anchor.y else area.height - anchor.y,
  )
  val side = max(
    (position.x - anchor.x) * corner.horizontal,
    (position.y - anchor.y) * corner.vertical,
  ).coerceAtMost(maxSide).coerceAtLeast(min(minSide, maxSide))
  return Rect(
    offset = Offset(
      x = if (corner.horizontal < 0) anchor.x - side else anchor.x,
      y = if (corner.vertical < 0) anchor.y - side else anchor.y,
    ),
    size = Size(side, side),
  )
}

private fun DrawScope.drawThirds(
  crop: Rect,
  color: Color,
) {
  val strokeWidth = 1.dp.toPx()
  for (i in 1..2) {
    val x = crop.left + crop.width * i / 3F
    val y = crop.top + crop.height * i / 3F
    drawLine(color, Offset(x, crop.top), Offset(x, crop.bottom), strokeWidth)
    drawLine(color, Offset(crop.left, y), Offset(crop.right, y), strokeWidth)
  }
}

private fun DrawScope.drawCornerBrackets(
  crop: Rect,
  strokeWidth: Float,
) {
  val radius = crop.width * FRAME_CORNER_RADIUS
  val length = min(16.dp.toPx(), crop.width * 0.15F)
  val bracket = Path().apply {
    moveTo(crop.left, crop.top + radius + length)
    lineTo(crop.left, crop.top + radius)
    arcTo(
      rect = Rect(crop.left, crop.top, crop.left + 2 * radius, crop.top + 2 * radius),
      startAngleDegrees = 180F,
      sweepAngleDegrees = 90F,
      forceMoveTo = false,
    )
    lineTo(crop.left + radius + length, crop.top)
  }
  val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
  // the frame is square, so turning the top left bracket around the center gives the others
  for (quarter in 0..3) {
    rotate(degrees = quarter * 90F, pivot = crop.center) {
      drawPath(bracket, Color.White, style = stroke)
    }
  }
}
