package voice.features.folderPicker.folderPicker

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
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.folders.FolderType
import voice.core.ui.rememberScoped
import voice.features.folderPicker.FolderTypeIcon
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@ContributesTo(AppScope::class)
interface FolderPickerGraph {
  val folderPickerViewModel: FolderPickerViewModel
}

@ContributesTo(AppScope::class)
interface FolderPickerProvider {

  @Provides
  @IntoSet
  fun folderPickerNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.FolderPicker> { key ->
    NavEntry(key) {
      FolderOverview()
    }
  }
}

@Composable
fun FolderOverview() {
  val viewModel: FolderPickerViewModel = rememberScoped {
    rootGraphAs<FolderPickerGraph>()
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
    onCloseClick = viewModel::onCloseClick,
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
