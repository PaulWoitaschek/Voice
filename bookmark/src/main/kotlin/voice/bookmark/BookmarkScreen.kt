package voice.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.squareup.anvil.annotations.ContributesTo
import voice.bookmark.dialogs.AddBookmarkDialog
import voice.bookmark.dialogs.EditBookmarkDialog
import voice.common.AppScope
import voice.common.BookId
import voice.common.compose.rememberScoped
import voice.common.rootComponentAs
import voice.data.Bookmark
import java.util.UUID
import voice.strings.R as StringsR

@ContributesTo(AppScope::class)
interface Component {
  val bookmarkViewModelFactory: BookmarkViewModel.Factory
}

@Composable
fun BookmarkScreen(bookId: BookId) {
  val viewModel = rememberScoped(bookId.value) {
    rootComponentAs<Component>().bookmarkViewModelFactory.create(bookId)
  }
  val viewState = viewModel.viewState()
  BookmarkScreen(
    viewState = viewState,
    onClose = viewModel::closeScreen,
    onAdd = viewModel::onAddClick,
    onDelete = viewModel::deleteBookmark,
    onEdit = viewModel::onEditClick,
    onScrollConfirm = viewModel::onScrollConfirm,
    onClick = viewModel::selectBookmark,
    onNewBookmarkNameChoose = viewModel::addBookmark,
    onCloseDialog = viewModel::closeDialog,
    onEditBookmark = viewModel::editBookmark,
  )
}

@Composable
internal fun BookmarkScreen(
  viewState: BookmarkViewState,
  onClose: () -> Unit,
  onAdd: () -> Unit,
  onDelete: (Bookmark.Id) -> Unit,
  onEdit: (Bookmark.Id) -> Unit,
  onScrollConfirm: () -> Unit,
  onClick: (Bookmark.Id) -> Unit,
  onCloseDialog: () -> Unit,
  onNewBookmarkNameChoose: (String) -> Unit,
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
        title = { Text(text = stringResource(id = StringsR.string.bookmark)) },
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
    floatingActionButton = {
      FloatingActionButton(
        onClick = onAdd,
        content = {
          Icon(Icons.Default.Add, contentDescription = stringResource(id = StringsR.string.add))
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

  when (viewState.dialogViewState) {
    BookmarkDialogViewState.AddBookmark -> {
      AddBookmarkDialog(
        onDismissRequest = onCloseDialog,
        onBookmarkNameChoose = onNewBookmarkNameChoose,
      )
    }
    BookmarkDialogViewState.None -> {
    }
    is BookmarkDialogViewState.EditBookmark -> {
      EditBookmarkDialog(
        onDismissRequest = onCloseDialog,
        onEditBookmark = onEditBookmark,
        bookmarkId = viewState.dialogViewState.id,
        initialTitle = viewState.dialogViewState.title ?: "",
      )
    }
  }
}

@Composable
internal fun BookmarkItem(
  bookmark: BookmarkItemViewState,
  onDelete: (Bookmark.Id) -> Unit,
  onEdit: (Bookmark.Id) -> Unit,
  onClick: (Bookmark.Id) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }

  SwipeToDismissBox(
    modifier = modifier,
    state = rememberSwipeToDismissBoxState(
      confirmValueChange = {
        when (it) {
          SwipeToDismissBoxValue.StartToEnd -> {
            onDelete(bookmark.id)
            true
          }
          SwipeToDismissBoxValue.EndToStart,
          SwipeToDismissBoxValue.Settled,
          -> false
        }
      },
    ),
    backgroundContent = {
      Box(
        Modifier
          .fillMaxSize()
          .background(Color.Red),
      ) {
        Icon(
          modifier = Modifier
            .padding(start = 16.dp)
            .align(Alignment.CenterStart),
          imageVector = Icons.Outlined.Delete,
          contentDescription = stringResource(id = StringsR.string.delete),
          tint = Color.White,
        )
      }
    },
    content = {
      ListItem(
        modifier = Modifier
          .clickable {
            onClick(bookmark.id)
          },
        headlineContent = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = bookmark.title)
            if (bookmark.showSleepIcon) {
              Icon(
                modifier = Modifier
                  .padding(start = 4.dp)
                  .size(16.dp),
                imageVector = Icons.Outlined.Timer,
                contentDescription = stringResource(StringsR.string.action_sleep),
              )
            }
          }
        },
        trailingContent = {
          Box {
            IconButton(
              onClick = {
                expanded = !expanded
              },
              content = {
                Icon(
                  imageVector = Icons.Default.MoreVert,
                  contentDescription = stringResource(id = StringsR.string.popup_edit),
                )
              },
            )
            DropdownMenu(
              expanded = expanded,
              onDismissRequest = { expanded = false },
            ) {
              DropdownMenuItem(
                text = { Text(stringResource(id = StringsR.string.popup_edit)) },
                onClick = {
                  expanded = false
                  onEdit(bookmark.id)
                },
              )
              DropdownMenuItem(
                text = { Text(stringResource(id = StringsR.string.remove)) },
                onClick = {
                  expanded = false
                  onDelete(bookmark.id)
                },
              )
            }
          }
        },
        supportingContent = {
          Text(text = bookmark.subtitle)
        },
      )
    },
  )
}

@Composable
@Preview
private fun BookmarkItemPreview() {
  BookmarkItem(
    bookmark = BookmarkItemViewState(
      title = "Bookmark 1",
      subtitle = "10:10:10 / 12:12:12",
      id = Bookmark.Id(UUID.randomUUID()),
      showSleepIcon = true,
    ),
    onDelete = {},
    onEdit = { },
    onClick = {},
  )
}
