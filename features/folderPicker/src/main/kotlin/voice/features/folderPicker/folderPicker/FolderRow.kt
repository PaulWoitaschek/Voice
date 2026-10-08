@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.folderPicker.folderPicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import voice.core.data.folders.FolderType
import voice.core.ui.ShapedIcon
import voice.core.ui.icons.VoiceIcons
import voice.features.folderPicker.icon
import voice.features.folderPicker.label
import voice.core.strings.R as StringsR

@Composable
internal fun FolderRow(
  item: FolderPickerViewState.Item,
  shape: Shape,
  onClick: (() -> Unit)?,
  onDeleteClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val style = item.folderType.style()
  Surface(
    modifier = modifier,
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    val changeLabel = stringResource(StringsR.string.folder_review_action_change)
    Row(
      modifier = Modifier
        .then(
          if (onClick != null) {
            Modifier.clickable(onClickLabel = changeLabel, onClick = onClick)
          } else {
            // still read as one, like the name and type of a clickable row
            Modifier.semantics(mergeDescendants = true) {}
          },
        )
        .heightIn(min = 80.dp)
        // the delete button brings its own room around the icon
        .padding(start = 12.dp, end = if (onDeleteClick != null) 4.dp else 16.dp, top = 12.dp, bottom = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ShapedIcon(
        icon = item.folderType.icon(),
        shape = style.shape,
        containerColor = style.color,
        contentColor = style.onColor,
      )
      Spacer(Modifier.width(16.dp))
      Column(
        modifier = Modifier.weight(1F),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = item.name,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        FolderTypeChip(folderType = item.folderType, style = style, changeable = onClick != null)
      }
      if (onDeleteClick != null) {
        Spacer(Modifier.width(4.dp))
        IconButton(
          onClick = onDeleteClick,
          shapes = IconButtonDefaults.shapes(),
        ) {
          Icon(
            imageVector = VoiceIcons.Delete,
            // every row has one, so say which folder goes
            contentDescription = "${stringResource(StringsR.string.common_action_delete)}, ${item.name}",
          )
        }
      }
    }
  }
}

@Composable
private fun FolderTypeChip(
  folderType: FolderType,
  style: FolderTypeStyle,
  changeable: Boolean,
) {
  Row(
    modifier = Modifier
      .background(style.container, CircleShape)
      .padding(start = 10.dp, end = if (changeable) 4.dp else 10.dp, top = 4.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      modifier = Modifier.weight(1F, fill = false),
      text = folderType.label(),
      style = MaterialTheme.typography.labelLarge,
      color = style.onContainer,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    if (changeable) {
      Icon(
        modifier = Modifier.size(18.dp),
        imageVector = VoiceIcons.ArrowDropDown,
        contentDescription = null,
        tint = style.onContainer,
      )
    }
  }
}

private class FolderTypeStyle(
  val shape: RoundedPolygon,
  val color: Color,
  val onColor: Color,
  val container: Color,
  val onContainer: Color,
)

@Composable
private fun FolderType.style(): FolderTypeStyle {
  val colors = MaterialTheme.colorScheme
  return when (this) {
    FolderType.Root -> FolderTypeStyle(
      shape = MaterialShapes.Cookie9Sided,
      color = colors.primary,
      onColor = colors.onPrimary,
      container = colors.primaryContainer,
      onContainer = colors.onPrimaryContainer,
    )
    FolderType.Author -> FolderTypeStyle(
      shape = MaterialShapes.Clover4Leaf,
      color = colors.tertiary,
      onColor = colors.onTertiary,
      container = colors.tertiaryContainer,
      onContainer = colors.onTertiaryContainer,
    )
    FolderType.SingleFolder -> FolderTypeStyle(
      shape = MaterialShapes.Cookie6Sided,
      color = colors.secondary,
      onColor = colors.onSecondary,
      container = colors.secondaryContainer,
      onContainer = colors.onSecondaryContainer,
    )
    FolderType.SingleFile -> FolderTypeStyle(
      shape = MaterialShapes.Sunny,
      color = colors.secondary,
      onColor = colors.onSecondary,
      container = colors.secondaryContainer,
      onContainer = colors.onSecondaryContainer,
    )
  }
}
