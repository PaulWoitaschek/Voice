package voice.features.playbackScreen.view

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import java.text.Bidi

/**
 * A single line that scrolls once if it doesn't fit, then rests at its start with the cut off end faded out.
 * It scrolls up to [bleed] beyond its bounds, into the padding around it.
 */
@Composable
internal fun MarqueeText(
  text: String,
  style: TextStyle,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  bleed: Dp = 0.dp,
) {
  var textWidth by remember { mutableIntStateOf(0) }
  // the marquee rests at the side of the layout direction, so an English title in an RTL layout would show its end
  val textDirection = remember(text) {
    if (Bidi(text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).baseIsLeftToRight()) LayoutDirection.Ltr else LayoutDirection.Rtl
  }
  CompositionLocalProvider(LocalLayoutDirection provides textDirection) {
    Text(
      text = text,
      style = style,
      color = color,
      maxLines = 1,
      onTextLayout = { textWidth = it.size.width },
      modifier = modifier
        .layout { measurable, constraints ->
          val bleedPx = bleed.roundToPx()
          val placeable = measurable.measure(constraints.offset(horizontal = 2 * bleedPx))
          layout(placeable.width - 2 * bleedPx, placeable.height) {
            placeable.place(-bleedPx, 0)
          }
        }
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
          drawContent()
          if (textWidth > size.width - 2 * bleed.roundToPx()) {
            val fade = 24.dp.toPx()
            val ltr = layoutDirection == LayoutDirection.Ltr
            drawRect(
              brush = Brush.horizontalGradient(
                colors = if (ltr) listOf(Color.Black, Color.Transparent) else listOf(Color.Transparent, Color.Black),
                startX = if (ltr) size.width - fade else 0F,
                endX = if (ltr) size.width else fade,
              ),
              blendMode = BlendMode.DstIn,
            )
          }
        }
        .basicMarquee(iterations = 1)
        .padding(horizontal = bleed),
    )
  }
}
