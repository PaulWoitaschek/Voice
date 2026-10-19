package voice.features.onboarding.explanation

import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min

@Composable
internal fun FolderPickerGuide(
  clock: () -> Float,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Canvas(modifier.clearAndSetSemantics {}) {
    // with animations turned off the clock stays at zero, which then shows the first tap
    val time = (clock() + START) % CYCLE
    val width = min(min(size.width * 0.72F, size.height * 0.95F), 300.dp.toPx())
    val height = min(width * 0.8F, size.height * 0.92F)
    val card = Rect(Offset(center.x - width / 2, center.y - height / 2), Size(width, height))
    val cardCorner = CornerRadius(24.dp.toPx())
    val padding = width * 0.07F
    val headerHeight = 10.dp.toPx()

    val buttonSize = Size(width * 0.42F, height * 0.16F)
    val button = Rect(
      Offset(card.right - padding - buttonSize.width, card.bottom - padding - buttonSize.height),
      buttonSize,
    )
    val rowsTop = card.top + padding + headerHeight + padding * 0.5F
    val rowHeight = (button.top - padding * 0.5F - rowsTop) / 3
    val iconSize = min(rowHeight * 0.5F, 24.dp.toPx())
    val labelLeft = card.left + padding + iconSize + 12.dp.toPx()
    val labelWidth = card.right - padding - labelLeft

    val dialogSize = Size(width * 0.7F, height * 0.46F)
    val dialog = Rect(Offset(card.center.x - dialogSize.width / 2, card.center.y - dialogSize.height / 2), dialogSize)
    val dialogPadding = dialogSize.height * 0.16F
    val allowSize = Size(dialogSize.width * 0.32F, dialogSize.height * 0.24F)
    val allow = Rect(
      Offset(dialog.right - dialogPadding - allowSize.width, dialog.bottom - dialogPadding - allowSize.height),
      allowSize,
    )

    val folder = Offset(labelLeft + labelWidth * 0.25F, rowsTop + rowHeight / 2)
    val taps = listOf(TAP_FOLDER to folder, TAP_USE to button.center, TAP_ALLOW to allow.center)

    drawRoundRect(colors.surfaceContainerHigh, card.topLeft, card.size, cardCorner)

    val opened = progress(time, 0.7F, 1.1F) - progress(time, 5.4F, 5.9F)
    val highlight = progress(time, 0.35F, 0.55F) - progress(time, 0.7F, 1F)
    drawRoundRect(
      color = colors.secondaryContainer,
      topLeft = Offset(card.left + padding / 2, rowsTop + 2.dp.toPx()),
      size = Size(width - padding, rowHeight - 4.dp.toPx()),
      cornerRadius = CornerRadius(rowHeight / 2),
      alpha = highlight,
    )

    fun listing(
      header: Float,
      labels: List<Float>,
      iconColor: Color,
      alpha: Float,
      shift: Float,
    ) {
      if (alpha <= 0F) return
      translate(left = shift) {
        drawRoundRect(
          color = colors.onSurface,
          topLeft = Offset(card.left + padding, card.top + padding),
          size = Size(width * header, headerHeight),
          cornerRadius = CornerRadius(headerHeight / 2),
          alpha = alpha * 0.8F,
        )
        labels.forEachIndexed { index, label ->
          val rowCenter = rowsTop + rowHeight * (index + 0.5F)
          drawFolder(Offset(card.left + padding, rowCenter - iconSize / 2), iconSize, iconColor, alpha)
          drawRoundRect(
            color = colors.onSurfaceVariant,
            topLeft = Offset(labelLeft, rowCenter - 4.dp.toPx()),
            size = Size(labelWidth * label, 8.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx()),
            alpha = alpha * 0.45F,
          )
        }
      }
    }
    clipRect(card.left, card.top, card.right, card.bottom) {
      val slide = 16.dp.toPx()
      listing(
        header = 0.42F,
        labels = listOf(0.5F, 0.72F, 0.38F),
        iconColor = colors.secondary,
        alpha = 1F - opened,
        shift =
        -slide * opened,
      )
      listing(header = 0.3F, labels = listOf(0.7F, 0.5F, 0.62F), iconColor = colors.tertiary, alpha = opened, shift = slide * (1F - opened))
    }

    // like in Android's picker, a folder can only be used once it's opened
    scale(1F - 0.08F * press(time, TAP_USE), pivot = button.center) {
      val buttonAlpha = 0.38F + 0.62F * opened
      drawRoundRect(colors.primary, button.topLeft, button.size, CornerRadius(button.height / 2), alpha = buttonAlpha)
      drawLabel(button, colors.onPrimary, buttonAlpha)
    }

    val dialogShown = progress(time, 1.7F, 2.1F, EaseOutBack) - progress(time, 3.1F, 3.4F)
    if (dialogShown > 0F) {
      val alpha = dialogShown.coerceIn(0F, 1F)
      drawRoundRect(colors.scrim, card.topLeft, card.size, cardCorner, alpha = 0.3F * alpha)
      scale(0.85F + 0.15F * dialogShown, pivot = dialog.center) {
        drawRoundRect(colors.surfaceContainerHighest, dialog.topLeft, dialog.size, CornerRadius(20.dp.toPx()), alpha = alpha)
        drawRoundRect(
          color = colors.onSurface,
          topLeft = Offset(dialog.left + dialogPadding, dialog.top + dialogPadding),
          size = Size(dialog.width * 0.6F, 9.dp.toPx()),
          cornerRadius = CornerRadius(4.5F.dp.toPx()),
          alpha = alpha * 0.8F,
        )
        drawRoundRect(
          color = colors.onSurfaceVariant,
          topLeft = Offset(dialog.left + dialogPadding, dialog.top + dialogPadding + 19.dp.toPx()),
          size = Size(dialog.width * 0.75F, 7.dp.toPx()),
          cornerRadius = CornerRadius(3.5F.dp.toPx()),
          alpha = alpha * 0.45F,
        )
        scale(1F - 0.1F * press(time, TAP_ALLOW), pivot = allow.center) {
          drawRoundRect(colors.primary, allow.topLeft, allow.size, CornerRadius(allow.height / 2), alpha = alpha)
          drawLabel(allow, colors.onPrimary, alpha)
        }
      }
    }

    val done = progress(time, 3.2F, 3.6F, EaseOutBack) - progress(time, 5.4F, 5.8F)
    if (done > 0F) {
      val radius = height * 0.2F * done
      drawCircle(colors.tertiary, radius, card.center)
      val check = Path().apply {
        moveTo(card.center.x - radius * 0.38F, card.center.y + radius * 0.02F)
        lineTo(card.center.x - radius * 0.1F, card.center.y + radius * 0.3F)
        lineTo(card.center.x + radius * 0.4F, card.center.y - radius * 0.26F)
      }
      drawPath(check, colors.onTertiary, style = Stroke(width = radius * 0.16F, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    val pointerAlpha = progress(time, 0F, 0.25F) - progress(time, 3.05F, 3.3F)
    val pointer = lerp(lerp(folder, button.center, progress(time, 0.75F, 1.4F)), allow.center, progress(time, 2.15F, 2.8F))
    val pointerRadius = 12.dp.toPx()
    taps.forEach { (at, position) ->
      val ripple = (time - at) / 0.45F
      if (ripple in 0F..1F) {
        drawCircle(
          color = colors.onSurface,
          radius = pointerRadius + 18.dp.toPx() * ripple,
          center = position,
          alpha = 0.4F * (1F - ripple),
          style = Stroke(width = 2.dp.toPx()),
        )
      }
    }
    val pressed = taps.sumOf { (at, _) -> press(time, at).toDouble() }.toFloat()
    drawCircle(colors.onSurface, pointerRadius * (1F - 0.15F * pressed), pointer, alpha = 0.35F * pointerAlpha)
  }
}

private fun DrawScope.drawFolder(
  topLeft: Offset,
  size: Float,
  color: Color,
  alpha: Float,
) {
  val corner = CornerRadius(size * 0.12F)
  drawRoundRect(color, topLeft, Size(size * 0.45F, size * 0.3F), corner, alpha = alpha)
  drawRoundRect(color, Offset(topLeft.x, topLeft.y + size * 0.16F), Size(size, size * 0.72F), corner, alpha = alpha)
}

private fun DrawScope.drawLabel(
  button: Rect,
  color: Color,
  alpha: Float,
) {
  val labelHeight = min(6.dp.toPx(), button.height * 0.3F)
  drawRoundRect(
    color = color,
    topLeft = Offset(button.center.x - button.width * 0.28F, button.center.y - labelHeight / 2),
    size = Size(button.width * 0.56F, labelHeight),
    cornerRadius = CornerRadius(labelHeight / 2),
    alpha = alpha * 0.85F,
  )
}

private fun progress(
  time: Float,
  start: Float,
  end: Float,
  easing: Easing = FastOutSlowInEasing,
): Float = easing.transform(((time - start) / (end - start)).coerceIn(0F, 1F))

private fun press(
  time: Float,
  at: Float,
): Float = (1F - abs(time - at) / 0.15F).coerceAtLeast(0F)

private const val CYCLE = 6.5F
private const val START = 0.6F
private const val TAP_FOLDER = 0.45F
private const val TAP_USE = 1.55F
private const val TAP_ALLOW = 2.95F
