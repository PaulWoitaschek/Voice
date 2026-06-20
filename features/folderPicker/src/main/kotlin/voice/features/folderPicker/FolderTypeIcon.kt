package voice.features.folderPicker

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import voice.core.data.folders.FolderType
import voice.core.ui.icons.AudioFile
import voice.core.ui.icons.Folder
import voice.core.ui.icons.LibraryBooks
import voice.core.ui.icons.Person
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

@Composable
internal fun FolderTypeIcon(folderType: FolderType) {
  Icon(
    imageVector = folderType.icon(),
    contentDescription = folderType.contentDescription(),
  )
}

private fun FolderType.icon(): ImageVector = when (this) {
  FolderType.SingleFile -> VoiceIcons.AudioFile
  FolderType.SingleFolder -> VoiceIcons.Folder
  FolderType.Root -> VoiceIcons.LibraryBooks
  FolderType.Author -> VoiceIcons.Person
}

@Composable
private fun FolderType.contentDescription(): String {
  val res = when (this) {
    FolderType.SingleFile,
    FolderType.SingleFolder,
    -> StringsR.string.folder_mode_single_title
    FolderType.Root -> StringsR.string.folder_mode_root_title
    FolderType.Author -> StringsR.string.folder_mode_author_title
  }
  return stringResource(res)
}
