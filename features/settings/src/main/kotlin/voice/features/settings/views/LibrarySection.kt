@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.settings.views

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

/**
 * The audiobook folders, showing their names, and the layout of the library, picked by looking at
 * little previews of it.
 */
@Composable
internal fun LibrarySection(
  folderNames: List<String>,
  useGrid: Boolean,
  onFoldersClick: () -> Unit,
  onUseGridChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  // the tertiary container is a light tone in the dark theme, which glares as a whole island
  val dark = colors.surface.luminance() < 0.5F
  SettingsIsland(
    modifier = modifier,
    title = stringResource(StringsR.string.settings_library_title),
    containerColor = if (dark) colors.tertiary.copy(alpha = 0.2F).compositeOver(colors.surfaceContainer) else colors.tertiaryContainer,
    contentColor = if (dark) colors.onSurface else colors.onTertiaryContainer,
  ) {
    IslandRow(
      title = stringResource(StringsR.string.library_folders_title),
      onClick = onFoldersClick,
      leading = {
        ShapedIcon(
          icon = VoiceIcons.Folder,
          shape = MaterialShapes.Cookie6Sided,
          containerColor = MaterialTheme.colorScheme.tertiary,
          contentColor = MaterialTheme.colorScheme.onTertiary,
        )
      },
      summary = { FolderNames(folderNames) },
    )
    Spacer(Modifier.height(12.dp))
    Row(
      modifier = Modifier
        .selectableGroup()
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      LayoutTile(
        modifier = Modifier.weight(1F),
        grid = false,
        selected = !useGrid,
        onClick = { onUseGridChange(false) },
      )
      LayoutTile(
        modifier = Modifier.weight(1F),
        grid = true,
        selected = useGrid,
        onClick = { onUseGridChange(true) },
      )
    }
  }
}

/** The first few folder names as little chips, or what this row is about while there are none. */
@Composable
private fun FolderNames(folderNames: List<String>) {
  if (folderNames.isEmpty()) {
    Text(stringResource(StringsR.string.settings_library_folders_summary))
    return
  }
  FlowRow(
    modifier = Modifier
      .padding(top = 4.dp)
      .animateContentSize(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    folderNames.take(MAX_FOLDER_CHIPS).forEach { name ->
      FolderChip(name)
    }
    if (folderNames.size > MAX_FOLDER_CHIPS) {
      FolderChip("+${folderNames.size - MAX_FOLDER_CHIPS}")
    }
  }
}

@Composable
private fun FolderChip(text: String) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14F),
    contentColor = LocalContentColor.current,
  ) {
    Text(
      modifier = Modifier
        .widthIn(max = 160.dp)
        .padding(horizontal = 10.dp, vertical = 4.dp),
      text = text,
      style = MaterialTheme.typography.labelMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

private const val MAX_FOLDER_CHIPS = 3

/**
 * A tiny library, as a list or as a [grid] of covers. Selecting it makes the covers pop in one
 * after another.
 */
@Composable
private fun LayoutTile(
  grid: Boolean,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val scale by animateFloatAsState(
    targetValue = if (selected) 1F else 0.92F,
    animationSpec = spring(dampingRatio = 0.45F, stiffness = Spring.StiffnessMediumLow),
    label = "layoutTileScale",
  )
  val borderWidth by animateDpAsState(
    targetValue = if (selected) 3.dp else 1.dp,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "layoutTileBorderWidth",
  )
  val borderColor by animateColorAsState(
    targetValue = if (selected) colors.tertiary else colors.outlineVariant,
    label = "layoutTileBorderColor",
  )
  val pop = remember { Animatable(1F) }
  var wasSelected by remember { mutableStateOf(selected) }
  LaunchedEffect(selected) {
    if (selected && !wasSelected) {
      pop.snapTo(0F)
      pop.animateTo(1F, tween(durationMillis = 800, easing = LinearEasing))
    } else {
      // deselected while still popping in, which would leave covers missing
      pop.snapTo(1F)
    }
    wasSelected = selected
  }
  val shape = RoundedCornerShape(20.dp)
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .selectable(
        selected = selected,
        onClick = onClick,
        role = Role.RadioButton,
      ),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1.1F)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        },
    ) {
      Canvas(
        Modifier
          .fillMaxSize()
          .clip(shape)
          .border(borderWidth, borderColor, shape),
      ) {
        drawRect(colors.surface)
        if (grid) {
          drawMiniGrid(colors, pop.value)
        } else {
          drawMiniList(colors, pop.value)
        }
      }
      CheckBadge(
        visible = selected,
        containerColor = colors.tertiary,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp),
      )
    }
    Spacer(Modifier.height(8.dp))
    Text(
      text = stringResource(
        if (grid) StringsR.string.settings_library_layout_grid else StringsR.string.settings_library_layout_list,
      ),
      style = MaterialTheme.typography.labelLarge,
      color = LocalContentColor.current.copy(alpha = if (selected) 1F else 0.7F),
    )
  }
}

private fun ColorScheme.coverColors(): List<Color> = listOf(
  primary,
  tertiary,
  secondary,
  primaryFixedDim,
  tertiaryFixedDim,
  secondaryFixedDim,
)

/** Covers with higher [index] pop in later as [pop] runs from 0 to 1. */
private fun coverScale(
  pop: Float,
  index: Int,
): Float {
  val local = ((pop - index * 0.1F) / 0.5F).coerceIn(0F, 1F)
  return EaseOutBack.transform(local)
}

private fun DrawScope.drawMiniGrid(
  colors: ColorScheme,
  pop: Float,
) {
  val coverColors = colors.coverColors()
  val padding = size.width * 0.12F
  val gap = size.width * 0.07F
  val columns = 3
  val coverSize = (size.width - 2 * padding - (columns - 1) * gap) / columns
  val lineHeight = size.height * 0.035F
  val rowHeight = coverSize + lineHeight * 3.4F
  val firstTop = (size.height - rowHeight - coverSize - lineHeight * 2) / 2
  var index = 0
  for (row in 0 until 2) {
    val top = firstTop + row * rowHeight
    for (column in 0 until columns) {
      val left = padding + column * (coverSize + gap)
      val coverScale = coverScale(pop, index)
      scale(coverScale, pivot = Offset(left + coverSize / 2, top + coverSize / 2)) {
        drawRoundRect(
          color = coverColors[index % coverColors.size],
          topLeft = Offset(left, top),
          size = Size(coverSize, coverSize),
          cornerRadius = CornerRadius(coverSize * 0.2F),
        )
      }
      drawRoundRect(
        color = colors.onSurfaceVariant,
        alpha = 0.5F,
        topLeft = Offset(left, top + coverSize + lineHeight),
        size = Size(coverSize * 0.8F, lineHeight),
        cornerRadius = CornerRadius(lineHeight / 2),
      )
      index++
    }
  }
}

private fun DrawScope.drawMiniList(
  colors: ColorScheme,
  pop: Float,
) {
  val coverColors = colors.coverColors()
  val padding = size.width * 0.12F
  val rows = 4
  val rowHeight = (size.height - 2 * padding) / rows
  val coverSize = rowHeight * 0.72F
  val lineHeight = size.height * 0.035F
  for (row in 0 until rows) {
    val top = padding + row * rowHeight + (rowHeight - coverSize) / 2
    val coverScale = coverScale(pop, row)
    scale(coverScale, pivot = Offset(padding + coverSize / 2, top + coverSize / 2)) {
      drawRoundRect(
        color = coverColors[row % coverColors.size],
        topLeft = Offset(padding, top),
        size = Size(coverSize, coverSize),
        cornerRadius = CornerRadius(coverSize * 0.2F),
      )
    }
    val textLeft = padding + coverSize + size.width * 0.06F
    val textWidth = size.width - textLeft - padding
    drawRoundRect(
      color = colors.onSurface,
      topLeft = Offset(textLeft, top + coverSize * 0.18F),
      size = Size(textWidth * if (row % 2 == 0) 0.9F else 0.7F, lineHeight),
      cornerRadius = CornerRadius(lineHeight / 2),
    )
    drawRoundRect(
      color = colors.secondaryContainer,
      topLeft = Offset(textLeft, top + coverSize * 0.62F),
      size = Size(textWidth, lineHeight),
      cornerRadius = CornerRadius(lineHeight / 2),
    )
    drawRoundRect(
      color = colors.primary,
      topLeft = Offset(textLeft, top + coverSize * 0.62F),
      size = Size(textWidth * (0.25F + 0.2F * row), lineHeight),
      cornerRadius = CornerRadius(lineHeight / 2),
    )
  }
}
