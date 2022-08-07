package voice.folderPicker

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope
import voice.common.compose.rememberScoped
import voice.common.rootComponentAs
import voice.data.folders.FolderType
import voice.logging.core.Logger

@ContributesTo(AppScope::class)
interface FolderPickerComponent {
  val folderPickerViewModel: FolderPickerViewModel
}

@Composable
fun FolderPicker(
  onCloseClick: () -> Unit,
) {
  val viewModel = rememberScoped {
    rootComponentAs<FolderPickerComponent>()
      .folderPickerViewModel
  }
  val viewState = viewModel.viewState()
  var launchedFolderType by remember {
    mutableStateOf<FolderType?>(null)
  }
  val onLaunchResult: (Uri?) -> Unit = { uri ->
    val folderType = launchedFolderType
    if (uri != null && folderType != null) {
      viewModel.add(uri, folderType)
    }
  }
  val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), onLaunchResult)
  val documentTreeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree(), onLaunchResult)
  FolderPickerView(
    viewState = viewState,
    onAddClick = { folderType ->
      launchedFolderType = folderType
      when (folderType) {
        FolderType.SingleFile -> {
          try {
            openDocumentLauncher.launch(arrayOf("audio/*", "video/*"))
          } catch (e: ActivityNotFoundException) {
            Logger.e(e, "No activity found for ACTION_OPEN_DOCUMENT. Broken device.")
          }
        }
        FolderType.SingleFolder,
        FolderType.Root,
        -> {
          try {
            documentTreeLauncher.launch(null)
          } catch (e: ActivityNotFoundException) {
            Logger.e(e, "No activity found for ACTION_OPEN_DOCUMENT_TREE. Broken device.")
          }
        }
      }
    },
    onDeleteClick = {
      viewModel.removeFolder(it)
    },
    onCloseClick = onCloseClick,
  )
}

@Composable
fun FolderPickerView(
  viewState: FolderPickerViewState,
  onAddClick: (FolderType) -> Unit,
  onDeleteClick: (FolderPickerViewState.Item) -> Unit,
  onCloseClick: () -> Unit,
) {
  Scaffold(
    topBar = {
      MediumTopAppBar(
        title = {
          Text(text = stringResource(R.string.audiobook_folders_title))
        },
        navigationIcon = {
          IconButton(onClick = onCloseClick) {
            Icon(
              imageVector = Icons.Outlined.ArrowBack,
              contentDescription = stringResource(R.string.close),
            )
          }
        },
      )
    },
    floatingActionButton = {
      Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.End,
      ) {
        FolderType.values().forEach { folderType ->
          FolderButton(
            folderType = folderType,
            onAddClick = onAddClick,
          )
        }
      }
    },
  ) {
    LazyColumn(contentPadding = it) {
      item { Spacer(modifier = Modifier.size(16.dp)) }
      items(viewState.items) { item ->
        ListItem(
          icon = {
            Icon(
              imageVector = item.folderType.icon(),
              contentDescription = item.folderType.text(),
            )
          },
          trailing = {
            IconButton(
              onClick = {
                onDeleteClick(item)
              },
              content = {
                Icon(
                  imageVector = Icons.Outlined.Delete,
                  contentDescription = stringResource(R.string.delete),
                )
              },
            )
          },
        ) {
          Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
        }
      }
    }
  }
}

@Composable
private fun FolderButton(
  folderType: FolderType,
  onAddClick: (FolderType) -> Unit,
) {
  val text = folderType.text()
  ExtendedFloatingActionButton(
    text = {
      Text(text)
    },
    onClick = {
      onAddClick(folderType)
    },
    icon = {
      Icon(
        imageVector = folderType.icon(),
        contentDescription = text,
      )
    },
  )
}

@Composable
private fun FolderType.icon(): ImageVector = when (this) {
  FolderType.SingleFile -> Icons.Outlined.AudioFile
  FolderType.SingleFolder -> Icons.Outlined.Folder
  FolderType.Root -> Icons.Outlined.LibraryBooks
}

@Composable
private fun FolderType.text(): String {
  val res = when (this) {
    FolderType.SingleFile -> R.string.folder_type_single_file
    FolderType.SingleFolder -> R.string.folder_type_single_folder
    FolderType.Root -> R.string.folder_type_audiobooks
  }
  return stringResource(res)
}

@Composable
@Preview
fun FolderPickerPreview() {
  FolderPickerView(
    viewState = FolderPickerViewState(
      items = listOf(
        FolderPickerViewState.Item(
          name = "My Audiobooks",
          id = Uri.EMPTY,
          folderType = FolderType.Root,
        ),
        FolderPickerViewState.Item(
          name = "Bobiverse 1-4",
          id = Uri.EMPTY,
          folderType = FolderType.SingleFolder,
        ),
        FolderPickerViewState.Item(
          name = "Harry Potter 1",
          id = Uri.EMPTY,
          folderType = FolderType.SingleFile,
        ),
      ),
    ),
    onAddClick = { },
    onDeleteClick = {},
  ) {
  }
}
