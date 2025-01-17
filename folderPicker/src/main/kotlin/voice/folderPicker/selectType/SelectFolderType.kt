package voice.folderPicker.selectType

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope
import voice.common.compose.rememberScoped
import voice.common.navigation.Destination
import voice.common.rootComponentAs
import voice.strings.R as StringsR

@ContributesTo(AppScope::class)
interface SelectFolderTypeComponent {
  val selectFolderTypeViewModelFactory: SelectFolderTypeViewModel.Factory
}

@Composable
fun SelectFolderType(
  uri: Uri,
  mode: Destination.SelectFolderType.Mode,
) {
  val context = LocalContext.current
  val viewModel = rememberScoped {
    rootComponentAs<SelectFolderTypeComponent>().selectFolderTypeViewModelFactory
      .create(
        uri = uri,
        mode = mode,
        documentFile = DocumentFile.fromTreeUri(context, uri)!!,
      )
  }
  SelectFolderType(
    viewState = viewModel.viewState(),
    onFolderModeSelect = viewModel::setFolderMode,
    onAddClick = viewModel::add,
    onBackClick = viewModel::onCloseClick,
  )
}

@Composable
private fun SelectFolderType(
  viewState: SelectFolderTypeViewState,
  onFolderModeSelect: (FolderMode) -> Unit,
  onAddClick: () -> Unit,
  onBackClick: () -> Unit,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    floatingActionButton = {
      AddingFab(addButtonVisible = viewState.addButtonVisible, onAddClick = onAddClick)
    },
    topBar = {
      AppBar(scrollBehavior, onBackClick)
    },
  ) { contentPadding ->
    Content(
      contentPadding = contentPadding,
      onFolderModeSelect = onFolderModeSelect,
      viewState = viewState,
    )
  }
}

@Composable
private fun Content(
  contentPadding: PaddingValues,
  viewState: SelectFolderTypeViewState,
  onFolderModeSelect: (FolderMode) -> Unit,
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(150.dp),
    contentPadding = contentPadding,
    modifier = Modifier.fillMaxSize(),
  ) {
    item(
      key = "header",
      span = { GridItemSpan(maxLineSpan) },
    ) {
      FolderModeSelectionCard(
        onFolderModeSelect = onFolderModeSelect,
        selectedFolderMode = viewState.selectedFolderMode,
      )
    }
    item(
      key = "folderStructureExplanation",
      span = { GridItemSpan(maxLineSpan) },
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
          modifier = Modifier.padding(top = 24.dp),
          text = stringResource(id = StringsR.string.folder_type_book_example_header),
          style = MaterialTheme.typography.headlineSmall,
        )
      }
    }
    if (viewState.loading) {
      item(
        key = "loading",
        span = { GridItemSpan(maxLineSpan) },
      ) {
        Box(Modifier.padding(top = 24.dp)) {
          CircularProgressIndicator(
            Modifier
              .size(48.dp)
              .align(Alignment.Center),
          )
        }
      }
    } else {
      if (viewState.noBooksDetected) {
        item(
          key = "noBooksDetected",
          span = { GridItemSpan(maxLineSpan) },
        ) {
          Text(text = stringResource(id = StringsR.string.folder_type_no_books))
        }
      } else {
        item(span = { GridItemSpan(maxLineSpan) }) {
          Spacer(modifier = Modifier.size(16.dp))
        }
        items(viewState.books) { book ->
          FolderModeBook(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, end = 8.dp, start = 8.dp),
            book = book,
          )
        }
        item { Spacer(modifier = Modifier.size(24.dp)) }
      }
    }
  }
}

@Composable
private fun AppBar(
  scrollBehavior: TopAppBarScrollBehavior,
  onBackClick: () -> Unit,
) {
  MediumTopAppBar(
    scrollBehavior = scrollBehavior,
    navigationIcon = {
      IconButton(onClick = onBackClick) {
        Icon(
          imageVector = Icons.Outlined.Close,
          contentDescription = stringResource(id = StringsR.string.close),
        )
      }
    },
    title = {
      Text(text = stringResource(id = StringsR.string.folder_type_title))
    },
  )
}

@Preview
@Composable
private fun SelectFolderTypePreview() {
  SelectFolderType(
    onBackClick = {},
    onFolderModeSelect = {},
    viewState = SelectFolderTypeViewState(
      books = listOf(
        SelectFolderTypeViewState.Book("Cats", 42),
        SelectFolderTypeViewState.Book("Dogs", 12),
      ),
      selectedFolderMode = FolderMode.SingleBook,
      noBooksDetected = false,
      loading = false,
      addButtonVisible = true,
    ),
    onAddClick = {},
  )
}
