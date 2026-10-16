package voice.features.folderPicker.folderPicker

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import voice.core.data.folders.FolderType
import voice.core.logging.api.Logger

// the picker starts where the folder or file was. Picking the same one again brings its books back as they were.
@Composable
internal fun rememberAllowAccess(
  onPick: (uri: Uri, folderType: FolderType, pickedUri: Uri) -> Unit,
): (FolderPickerViewState.Item) -> Unit {
  // the picker can outlive the process
  var uri by rememberSaveable { mutableStateOf<Uri?>(null) }
  var folderType by rememberSaveable { mutableStateOf<FolderType?>(null) }
  val onResult: (Uri?) -> Unit = { pickedUri ->
    val uri = uri
    val folderType = folderType
    if (pickedUri != null && uri != null && folderType != null) {
      onPick(uri, folderType, pickedUri)
    }
  }
  val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree(), onResult)
  val fileLauncher = rememberLauncherForActivityResult(OpenDocumentAt(), onResult)
  return { item ->
    uri = item.id
    folderType = item.folderType
    try {
      when (item.folderType) {
        FolderType.SingleFile -> fileLauncher.launch(item.id)
        FolderType.SingleFolder,
        FolderType.Root,
        FolderType.Author,
        -> {
          // the picker ignores a tree uri as the place to start
          treeLauncher.launch(DocumentsContract.buildDocumentUriUsingTree(item.id, DocumentsContract.getTreeDocumentId(item.id)))
        }
      }
    } catch (e: ActivityNotFoundException) {
      Logger.w(e, "Could not allow access again")
    }
  }
}

private class OpenDocumentAt : ActivityResultContract<Uri, Uri?>() {

  override fun createIntent(
    context: Context,
    input: Uri,
  ): Intent {
    return Intent(Intent.ACTION_OPEN_DOCUMENT)
      .addCategory(Intent.CATEGORY_OPENABLE)
      .setType("*/*")
      .putExtra(DocumentsContract.EXTRA_INITIAL_URI, input)
  }

  override fun parseResult(
    resultCode: Int,
    intent: Intent?,
  ): Uri? = intent.takeIf { resultCode == Activity.RESULT_OK }?.data
}
