package voice.folderPicker

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import voice.data.folders.FolderType
import voice.strings.R as StringsR

@Composable
internal fun FolderTypeIcon(folderType: FolderType) {
  Icon(
    imageVector = folderType.icon(),
    contentDescription = folderType.contentDescription(),
  )
}

private fun FolderType.icon(): ImageVector = when (this) {
  FolderType.SingleFile -> Icons.Outlined.AudioFile
  FolderType.SingleFolder -> Icons.Outlined.Folder
  FolderType.Root -> Icons.Outlined.LibraryBooks
}

@Composable
private fun FolderType.contentDescription(): String {
  val res = when (this) {
    FolderType.SingleFile -> StringsR.string.folder_type_single_file
    FolderType.SingleFolder -> StringsR.string.folder_type_single_folder
    FolderType.Root -> StringsR.string.folder_type_audiobooks
  }
  return stringResource(res)
}
