package voice.features.folderPicker.addcontent

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import voice.core.logging.api.Logger
import voice.core.ui.OnboardingButton
import voice.features.folderPicker.folderPicker.FileTypeSelection
import voice.core.strings.R as StringsR

/**
 * Picking a folder is the way to go: it can hold one book or a whole library. A single file is
 * the exception, so it's a quieter text button.
 */
@Composable
internal fun ColumnScope.ContentTypeChoices(onAdd: (FileTypeSelection, Uri) -> Unit) {
  val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) {
      onAdd(FileTypeSelection.File, uri)
    }
  }
  val documentTreeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
    if (uri != null) {
      onAdd(FileTypeSelection.Folder, uri)
    }
  }
  OnboardingButton(
    text = stringResource(StringsR.string.folder_add_action_folder),
    onClick = {
      try {
        documentTreeLauncher.launch(null)
      } catch (e: ActivityNotFoundException) {
        Logger.w(e, "Could not add folder")
      }
    },
  )
  TextButton(
    modifier = Modifier.fillMaxWidth(),
    onClick = {
      try {
        openDocumentLauncher.launch(arrayOf("*/*"))
      } catch (e: ActivityNotFoundException) {
        Logger.w(e, "Could not add file")
      }
    },
  ) {
    Text(stringResource(StringsR.string.folder_add_action_file))
  }
}
