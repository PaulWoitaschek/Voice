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
    FolderType.SingleFile -> R.string.folder_type_single_file
    FolderType.SingleFolder -> R.string.folder_type_single_folder
    FolderType.Root -> R.string.folder_type_audiobooks
  }
  return stringResource(res)
}
