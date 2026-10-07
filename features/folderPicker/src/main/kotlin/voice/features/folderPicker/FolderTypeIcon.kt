package voice.features.folderPicker

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import voice.core.data.folders.FolderType
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

// decorative, the label next to it tells the type
@Composable
internal fun FolderTypeIcon(
  folderType: FolderType,
  modifier: Modifier = Modifier,
  tint: Color = LocalContentColor.current,
) {
  Icon(
    modifier = modifier,
    imageVector = folderType.icon(),
    contentDescription = null,
    tint = tint,
  )
}

private fun FolderType.icon(): ImageVector = when (this) {
  FolderType.SingleFile -> VoiceIcons.AudioFile
  FolderType.SingleFolder -> VoiceIcons.Folder
  FolderType.Root -> VoiceIcons.LibraryBooks
  FolderType.Author -> VoiceIcons.Person
}

@Composable
internal fun FolderType.label(): String {
  val res = when (this) {
    FolderType.SingleFile -> StringsR.string.folder_mode_file_label
    FolderType.SingleFolder -> StringsR.string.folder_mode_single_label
    FolderType.Root -> StringsR.string.folder_mode_root_label
    FolderType.Author -> StringsR.string.folder_mode_author_label
  }
  return stringResource(res)
}
