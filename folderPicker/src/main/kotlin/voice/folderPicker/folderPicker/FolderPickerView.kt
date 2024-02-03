package voice.folderPicker.folderPicker

import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import voice.strings.R as StringsR

@ContributesTo(AppScope::class)
interface FolderPickerComponent {
  val folderPickerViewModel: FolderPickerViewModel
}

@Composable
fun FolderOverview(onCloseClick: () -> Unit) {
  val viewModel: FolderPickerViewModel = rememberScoped {
    rootComponentAs<FolderPickerComponent>()
      .folderPickerViewModel
  }
  val viewState = viewModel.viewState()
  FolderOverviewView(
    viewState = viewState,
    onAddClick = {
      viewModel.add()
    },
    onDeleteClick = {
      viewModel.removeFolder(it)
    },
    onCloseClick = onCloseClick,
  )
}

@Composable
private fun FolderOverviewView(
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
          Text(text = stringResource(StringsR.string.audiobook_folders_title))
        },
        navigationIcon = {
          IconButton(onClick = onCloseClick) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = stringResource(StringsR.string.close),
            )
          }
        },
      )
    },
    floatingActionButton = {
      val text = stringResource(id = StringsR.string.add)
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
  ) { contentPadding ->
    LazyColumn(contentPadding = contentPadding) {
      item { Spacer(modifier = Modifier.size(16.dp)) }
      items(viewState.items) { item ->
        ListItem(
          leadingContent = {
            FolderTypeIcon(folderType = item.folderType)
          },
          trailingContent = {
            IconButton(
              onClick = {
                onDeleteClick(item)
              },
              content = {
                Icon(
                  imageVector = Icons.Outlined.Delete,
                  contentDescription = stringResource(StringsR.string.delete),
                )
              },
            )
          },
          headlineContent = {
            Text(text = item.name)
          },
        )
      }
    }
  }
}

@Suppress("ktlint:compose:preview-public-check")
@Composable
@Preview
fun FolderOverviewPreview() {
  FolderOverviewView(
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
