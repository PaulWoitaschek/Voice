package voice.common.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ln

@Composable
fun LongClickableCard(
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  shape: Shape = RoundedCornerShape(12.dp),
  border: BorderStroke? = null,
  elevation: CardElevation = CardDefaults.cardElevation(),
  colors: CardColors = CardDefaults.cardColors(),
  content: @Composable ColumnScope.() -> Unit
) {
  LongClickableSurface(
    onClick = onClick,
    onLongClick = onLongClick,
    modifier = modifier,
    enabled = enabled,
    shape = shape,
    color = colors.containerColor(enabled).value,
    contentColor = colors.contentColor(enabled).value,
    tonalElevation = elevation.tonalElevation(enabled, interactionSource).value,
    shadowElevation = elevation.shadowElevation(enabled, interactionSource).value,
    border = border,
    interactionSource = interactionSource,
  ) {
    Column(content = content)
  }
}

@Composable
private fun LongClickableSurface(
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  shape: Shape = Shapes.None,
  color: Color = MaterialTheme.colorScheme.surface,
  contentColor: Color = contentColorFor(color),
  tonalElevation: Dp = 0.dp,
  shadowElevation: Dp = 0.dp,
  border: BorderStroke? = null,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  content: @Composable () -> Unit
) {
  val absoluteElevation = LocalAbsoluteTonalElevation.current + tonalElevation
  CompositionLocalProvider(
    LocalContentColor provides contentColor,
    LocalAbsoluteTonalElevation provides absoluteElevation
  ) {
    Box(
      modifier = modifier
        .surface(
          shape = shape,
          backgroundColor = surfaceColorAtElevation(
            color = color,
            elevation = absoluteElevation
          ),
          border = border,
          shadowElevation = shadowElevation
        )
        .combinedClickable(
          interactionSource = interactionSource,
          indication = rememberRipple(),
          enabled = enabled,
          role = Role.Button,
          onClick = onClick,
          onLongClick = onLongClick,
        ),
      propagateMinConstraints = true
    ) {
      content()
    }
  }
}

internal fun ColorScheme.surfaceColorAtElevation(
  elevation: Dp,
): Color {
  if (elevation == 0.dp) return surface
  val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
  return surfaceTint.copy(alpha = alpha).compositeOver(surface)
}

private fun Modifier.surface(
  shape: Shape,
  backgroundColor: Color,
  border: BorderStroke?,
  shadowElevation: Dp
) = this
  .shadow(shadowElevation, shape, clip = false)
  .then(if (border != null) Modifier.border(border, shape) else Modifier)
  .background(color = backgroundColor, shape = shape)
  .clip(shape)

@Composable
private fun surfaceColorAtElevation(color: Color, elevation: Dp): Color {
  return if (color == MaterialTheme.colorScheme.surface) {
    MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
  } else {
    color
  }
}
