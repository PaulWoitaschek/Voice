package voice.folderPicker.addcontent

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.folderPicker.folderPicker.FileTypeSelection
import voice.logging.core.Logger
import voice.strings.R

@Composable
internal fun SelectFolderButtonRow(onAdd: (FileTypeSelection, Uri) -> Unit) {
  Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
  ) {
    val openDocumentLauncher = rememberLauncherForActivityResult(
      ActivityResultContracts.OpenDocument(),
    ) { uri ->
      if (uri != null) {
        onAdd(FileTypeSelection.File, uri)
      }
    }
    val documentTreeLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
          onAdd(FileTypeSelection.Folder, uri)
        }
      }

    SelectFolderButton(
      icon = Icons.Outlined.Folder,
      text = stringResource(id = R.string.select_folder_type_folder),
      onClick = {
        try {
          documentTreeLauncher.launch(null)
        } catch (e: ActivityNotFoundException) {
          Logger.e(e, "Could not add folder")
        }
      },
    )
    Spacer(modifier = Modifier.size(8.dp))
    SelectFolderButton(
      icon = Icons.Outlined.AudioFile,
      text = stringResource(id = R.string.select_folder_type_file),
      onClick = {
        try {
          openDocumentLauncher.launch(arrayOf("*/*"))
        } catch (e: ActivityNotFoundException) {
          Logger.e(e, "Could not add file")
        }
      },
    )
  }
}
