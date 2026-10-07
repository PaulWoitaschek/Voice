@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.folderPicker.folderPicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import voice.core.data.folders.FolderType
import voice.core.ui.icons.VoiceIcons
import voice.features.folderPicker.FolderTypeIcon
import voice.features.folderPicker.label
import voice.core.strings.R as StringsR

/**
 * A folder in an expressive group, leading with a sticker for how its books are found. Tapping it
 * changes that, so it's left out for a single file, which is always one book.
 */
@Composable
internal fun FolderRow(
  item: FolderPickerViewState.Item,
  shape: Shape,
  onClick: (() -> Unit)?,
  onDeleteClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    val changeLabel = stringResource(StringsR.string.folder_review_action_change)
    Row(
      modifier = Modifier
        .then(if (onClick != null) Modifier.clickable(onClickLabel = changeLabel, onClick = onClick) else Modifier)
        .heightIn(min = 80.dp)
        // the delete button brings its own room around the icon
        .padding(start = 12.dp, end = if (onDeleteClick != null) 4.dp else 16.dp, top = 12.dp, bottom = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FolderTypeBadge(item.folderType)
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
        FolderTypeChip(folderType = item.folderType, changeable = onClick != null)
      }
      if (onDeleteClick != null) {
        Spacer(Modifier.width(4.dp))
        IconButton(
          onClick = onDeleteClick,
          shapes = IconButtonDefaults.shapes(),
        ) {
          Icon(
            imageVector = VoiceIcons.Delete,
            contentDescription = stringResource(StringsR.string.common_action_delete),
          )
        }
      }
    }
  }
}

/** The icon of how a folder's books are found, like a sticker on a playful shape. */
@Composable
private fun FolderTypeBadge(folderType: FolderType) {
  val style = folderType.style()
  Box(
    modifier = Modifier
      .size(48.dp)
      .background(style.color, style.shape.toShape()),
    contentAlignment = Alignment.Center,
  ) {
    FolderTypeIcon(folderType = folderType, tint = style.onColor)
  }
}

/** How a folder's books are found. Folders that can change it get an arrow, like a dropdown. */
@Composable
private fun FolderTypeChip(
  folderType: FolderType,
  changeable: Boolean,
) {
  val style = folderType.style()
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

// libraries are primary or tertiary, folders and files that are one book are secondary
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
