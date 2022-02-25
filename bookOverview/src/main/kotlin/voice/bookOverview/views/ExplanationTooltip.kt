package voice.bookOverview.views

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import voice.logging.core.Logger

@Composable
internal fun ExplanationTooltip(content: @Composable ColumnScope.() -> Unit) {
  val density = LocalDensity.current
  val rightMargin = with(density) {
    16.dp.toPx()
  }
  var bookIconCenterX: Float? by remember { mutableStateOf(null) }
  var flagGlobalCenterX: Float? by remember { mutableStateOf(null) }
  Popup(popupPositionProvider = object : PopupPositionProvider {
    override fun calculatePosition(
      anchorBounds: IntRect,
      windowSize: IntSize,
      layoutDirection: LayoutDirection,
      popupContentSize: IntSize
    ): IntOffset {
      var offset = IntOffset(anchorBounds.center.x - popupContentSize.width / 2, anchorBounds.bottom)
      if ((offset.x + popupContentSize.width + rightMargin) > windowSize.width) {
        offset -= IntOffset(rightMargin.toInt() + (offset.x + popupContentSize.width - windowSize.width), 0)
      }

      bookIconCenterX = anchorBounds.center.x.toFloat()
      bookIconCenterX?.let { bookIconCenterX ->
        flagGlobalCenterX = bookIconCenterX - offset.x
      }
      return offset
    }
  }) {
    val triangleSize = with(density) {
      28.dp.toPx()
    }
    Card(
      modifier = Modifier
        .widthIn(max = 240.dp)
        .onGloballyPositioned {
          Logger.w("""positionInRoot=${it.positionInRoot()}, """)
        },
      shape = GenericShape { size, layoutDirection ->
        addOutline(RoundedCornerShape(12.0.dp).createOutline(size, layoutDirection, density))
        flagGlobalCenterX?.let { flagGlobalCenterX ->
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
    ) {
      content()
    }
  }
}
