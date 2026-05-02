package voice.features.bookmark

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.features.bookmark.dialogs.EditAudiologDialog
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@ContributesTo(AppScope::class)
interface AudiologProvider {

  @Provides
  @IntoSet
  fun audiologNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.Audiolog> { key ->
    NavEntry(key) {
      AudiologScreen(bookId = key.bookId)
    }
  }
}

@Composable
fun AudiologScreen(bookId: BookId) {
  val viewModel = retain(bookId.value) {
    rootGraphAs<Graph>().bookmarkViewModelFactory.create(bookId, audiolog = true)
  }
  val viewState = viewModel.viewState()
  AudiobookScreen(
    viewState = viewState,
    onClose = viewModel::closeScreen,
    onDelete = viewModel::deleteBookmark,
    onEdit = viewModel::onEditClick,
    onScrollConfirm = viewModel::onScrollConfirm,
    onClick = viewModel::selectBookmark,
    onCloseDialog = viewModel::closeDialog,
    onEditBookmark = viewModel::editBookmark,
  )
}

@Composable
internal fun AudiobookScreen(
  viewState: BookmarkViewState,
  onClose: () -> Unit,
  onDelete: (Bookmark.Id) -> Unit,
  onEdit: (Bookmark.Id) -> Unit,
  onScrollConfirm: () -> Unit,
  onClick: (Bookmark.Id) -> Unit,
  onCloseDialog: () -> Unit,
  onEditBookmark: (Bookmark.Id, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val snackbarHostState = remember { SnackbarHostState() }

  Scaffold(
    modifier = modifier,
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState)
    },
    topBar = {
      TopAppBar(
        title = { Text(text = stringResource(id = StringsR.string.audiolog)) },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = stringResource(id = StringsR.string.close),
            )
          }
        },
      )
    },
  ) { paddingValues ->
    val lazyListState = rememberLazyListState()
    LaunchedEffect(viewState.shouldScrollTo, onScrollConfirm) {
      val index = viewState.bookmarks.indexOfFirst { it.id == viewState.shouldScrollTo }
      if (index != -1) {
        lazyListState.animateScrollToItem(index)
        onScrollConfirm()
      }
    }
    LazyColumn(
      state = lazyListState,
      contentPadding = paddingValues,
    ) {
      items(
        items = viewState.bookmarks,
        key = { it.id.value.toString() },
      ) { bookmark ->
        BookmarkItem(
          modifier = Modifier.animateItem(),
          bookmark = bookmark,
          onDelete = onDelete,
          onEdit = onEdit,
          onClick = onClick,
        )
      }
      item {
        Spacer(Modifier.size(88.dp))
      }
    }
  }

  val dialog = viewState.dialogViewState
  if (dialog is BookmarkDialogViewState.EditBookmark) {
    EditAudiologDialog(
      onDismissRequest = onCloseDialog,
      onEditBookmark = onEditBookmark,
      bookmarkId = dialog.id,
      initialTitle = dialog.title ?: "",
    )
  }
}
