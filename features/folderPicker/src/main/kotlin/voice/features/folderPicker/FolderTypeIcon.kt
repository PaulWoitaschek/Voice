package voice.features.folderPicker

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import voice.core.data.folders.FolderType
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

// decorative, the label next to it tells the type
@Composable
internal fun FolderTypeIcon(
  folderType: FolderType,
  modifier: Modifier = Modifier,
) {
  Icon(
    modifier = modifier,
    painter = painterResource(folderType.icon()),
    contentDescription = null,
  )
}

@DrawableRes
internal fun FolderType.icon(): Int =
  when (this) {
    FolderType.SingleFile -> UiR.drawable.ic_audio_file
    FolderType.SingleFolder -> UiR.drawable.ic_folder
    FolderType.Root -> UiR.drawable.ic_library_books
    FolderType.Author -> UiR.drawable.ic_person
  }

@Composable
internal fun FolderType.label(): String {
  val res =
    when (this) {
      FolderType.SingleFile -> StringsR.string.folder_mode_file_label
      FolderType.SingleFolder -> StringsR.string.folder_mode_single_label
      FolderType.Root -> StringsR.string.folder_mode_root_label
      FolderType.Author -> StringsR.string.folder_mode_author_label
    }
  return stringResource(res)
}
