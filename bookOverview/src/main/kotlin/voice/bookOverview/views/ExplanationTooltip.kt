package voice.bookOverview.views

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider

@Composable
internal fun ExplanationTooltip(content: @Composable ColumnScope.() -> Unit) {
  var flagGlobalCenterX: Float? by remember { mutableStateOf(null) }
  val popupPositionProvider = ExplanationTooltipPopupPositionProvider(LocalDensity.current) {
    flagGlobalCenterX = it.toFloat()
  }
  Popup(popupPositionProvider = popupPositionProvider) {
    Card(
      modifier = Modifier.widthIn(max = 240.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      shape = explanationTooltipShape(flagGlobalCenterX, LocalDensity.current)
    ) {
      content()
    }
  }
}

private class ExplanationTooltipPopupPositionProvider(
  private val density: Density,
  private val onFlagCenterX: (Int) -> Unit,
) : PopupPositionProvider {
  override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
    val rightMargin = with(density) { 16.dp.toPx() }
    var offset = IntOffset(anchorBounds.center.x - popupContentSize.width / 2, anchorBounds.bottom)
    if ((offset.x + popupContentSize.width + rightMargin) > windowSize.width) {
      offset -= IntOffset(rightMargin.toInt() + (offset.x + popupContentSize.width - windowSize.width), 0)
    }

    onFlagCenterX(anchorBounds.center.x - offset.x)

    return offset
  }
}

private fun explanationTooltipShape(flagGlobalCenterX: Float?, density: Density): GenericShape {
  val triangleSize = with(density) {
    28.dp.toPx()
  }
  return GenericShape { size, layoutDirection ->
    addOutline(
      RoundedCornerShape(12.0.dp)
        .createOutline(size, layoutDirection, density)
    )
    if (flagGlobalCenterX != null) {
      val trianglePath = Path().apply {
        moveTo(
          x = flagGlobalCenterX - triangleSize / 2F,
          y = 0F
        )
        lineTo(
          x = flagGlobalCenterX,
          y = -triangleSize / 2F
        )
        lineTo(
          x = flagGlobalCenterX + triangleSize / 2F,
          y = 0F
        )
        close()
      }
      op(this, trianglePath, PathOperation.Union)
    }
  }
}
