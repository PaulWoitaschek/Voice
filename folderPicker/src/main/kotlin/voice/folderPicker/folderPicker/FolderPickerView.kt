package voice.folderPicker.folderPicker

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope
import voice.common.compose.rememberScoped
import voice.common.rootComponentAs
import voice.data.folders.FolderType
import voice.folderPicker.FolderTypeIcon
import voice.folderPicker.R

@ContributesTo(AppScope::class)
interface FolderPickerComponent {
  val folderPickerViewModel: FolderPickerViewModel
}

@Composable
fun FolderPicker(
  onCloseClick: () -> Unit,
) {
  val viewModel: FolderPickerViewModel = rememberScoped {
    rootComponentAs<FolderPickerComponent>()
      .folderPickerViewModel
  }
  val viewState = viewModel.viewState()

  var showSelectFileDialog by remember {
    mutableStateOf(false)
  }
  val openDocumentLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      viewModel.add(uri, FileTypeSelection.File)
    }
  }
  val documentTreeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
    if (uri != null) {
      viewModel.add(uri, FileTypeSelection.Folder)
    }
  }

  if (showSelectFileDialog) {
    FileTypeSelectionDialog(
      onDismiss = {
        showSelectFileDialog = false
      },
      onSelected = { selection ->
        when (selection) {
          FileTypeSelection.File -> {
            openDocumentLauncher.launch(arrayOf("*/*"))
          }
          FileTypeSelection.Folder -> {
            documentTreeLauncher.launch(null)
          }
        }
      },
    )
  }

  FolderPickerView(
    viewState = viewState,
    onAddClick = {
      showSelectFileDialog = true
    },
    onDeleteClick = {
      viewModel.removeFolder(it)
    },
    onCloseClick = onCloseClick,
  )
}

@Composable
private fun FolderPickerView(
  viewState: FolderPickerViewState,
  onAddClick: () -> Unit,
  onDeleteClick: (FolderPickerViewState.Item) -> Unit,
  onCloseClick: () -> Unit,
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      MediumTopAppBar(
        scrollBehavior = scrollBehavior,
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
      val text = stringResource(id = R.string.add)
      ExtendedFloatingActionButton(
        text = {
          Text(text)
        },
        onClick = {
          onAddClick()
        },
        icon = {
          Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = text,
          )
        },
      )
    },
  ) {
    LazyColumn(contentPadding = it) {
      item { Spacer(modifier = Modifier.size(16.dp)) }
      items(viewState.items) { item ->
        ListItem(
          icon = {
            FolderTypeIcon(folderType = item.folderType)
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
