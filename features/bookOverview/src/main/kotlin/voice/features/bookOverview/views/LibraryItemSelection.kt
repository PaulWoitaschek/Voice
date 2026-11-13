@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import voice.core.ui.R as UiR

internal fun Modifier.libraryItemClickable(
  selected: Boolean?,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
): Modifier =
  if (selected == null) {
    combinedClickable(onClick = onClick, onLongClick = onLongClick)
  } else {
    toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
  }

@Composable
internal fun LibraryTile(
  selected: Boolean?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val shape = MaterialTheme.shapes.largeIncreased
  Surface(
    modifier = modifier.clip(shape).libraryItemClickable(selected, onClick, onLongClick),
    shape = shape,
    color = selectedContainerColor(selected, Color.Transparent),
    contentColor =
      if (selected == true) MaterialTheme.colorScheme.onSecondaryContainer
      else LocalContentColor.current,
  ) {
    Column(Modifier.padding(6.dp), content = content)
  }
}

@Composable
internal fun selectedContainerColor(
  selected: Boolean?,
  default: Color,
): Color = if (selected == true) MaterialTheme.colorScheme.secondaryContainer else default

// the default track has the color of a selected item, so its empty part would disappear
@Composable
internal fun progressTrackColor(selected: Boolean?): Color =
  if (selected == true) MaterialTheme.colorScheme.surface
  else ProgressIndicatorDefaults.linearTrackColor

// an empty circle while selecting, so every item shows that it can be selected. It sits at the top,
// so it doesn't cover the badge at the bottom.
@Composable
internal fun BoxScope.SelectionBadge(
  selected: Boolean?,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val pop = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
  AnimatedVisibility(
    visible = selected != null,
    modifier = modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
    enter = scaleIn(pop),
    exit = scaleOut(MaterialTheme.motionScheme.fastEffectsSpec()),
  ) {
    val checked by
      animateFloatAsState(if (selected == true) 1F else 0F, pop, label = "selectionBadge")
    Box(
      modifier =
        Modifier.size(24.dp)
          .background(colors.surface, CircleShape)
          .padding(2.dp)
          .border(2.dp, colors.outline, CircleShape),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .graphicsLayer {
              // the spring bounces below zero, which would mirror the check
              val scale = checked.coerceAtLeast(0F)
              scaleX = scale
              scaleY = scale
            }
            .background(colors.primary, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          modifier = Modifier.size(14.dp),
          painter = painterResource(UiR.drawable.ic_check),
          contentDescription = null,
          tint = colors.onPrimary,
        )
      }
    }
  }
}
