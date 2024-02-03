package voice.folderPicker

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
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
  FolderType.Root -> Icons.AutoMirrored.Outlined.LibraryBooks
  FolderType.Author -> Icons.Outlined.Person
}

@Composable
private fun FolderType.contentDescription(): String {
  val res = when (this) {
    FolderType.SingleFile,
    FolderType.SingleFolder,
    -> StringsR.string.folder_mode_single
    FolderType.Root -> StringsR.string.folder_mode_root
    FolderType.Author -> StringsR.string.folder_mode_author
  }
  return stringResource(res)
}
