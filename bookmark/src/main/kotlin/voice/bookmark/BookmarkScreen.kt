package voice.bookmark

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import voice.bookmark.dialogs.AddBookmarkDialog
import voice.bookmark.dialogs.EditBookmarkDialog
import voice.data.Bookmark
import java.util.UUID
import kotlin.math.roundToInt
import voice.strings.R as StringsR

@Composable
internal fun BookmarkScreen(
  viewState: BookmarkViewState,
  onClose: () -> Unit,
  onAdd: () -> Unit,
  onDelete: (Bookmark.Id) -> Unit,
  onEdit: (Bookmark.Id) -> Unit,
  onScrollConfirmed: () -> Unit,
  onClick: (Bookmark.Id) -> Unit,
  onCloseDialog: () -> Unit,
  onNewBookmarkNameChosen: (String) -> Unit,
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
    LaunchedEffect(viewState.shouldScrollTo, onScrollConfirmed) {
      val index = viewState.bookmarks.indexOfFirst { it.id == viewState.shouldScrollTo }
      if (index != -1) {
        lazyListState.animateScrollToItem(index)
        onScrollConfirmed()
      }
    }
    LazyColumn(
      state = lazyListState,
      modifier = Modifier
        .padding(paddingValues)
        .fillMaxSize()
        .padding(bottom = 88.dp)
        .clipToBounds(),
    ) {
      items(
        items = viewState.bookmarks,
        key = { it.id.value.toString() },
      ) { bookmark ->
        BookmarkItem(
          modifier = Modifier.animateItemPlacement(),
          bookmark = bookmark,
          onDelete = onDelete,
          onEdit = onEdit,
          onClick = onClick,
        )
      }
    }
  }

  when (viewState.dialogViewState) {
    BookmarkDialogViewState.AddBookmark -> {
      AddBookmarkDialog(
        onDismissRequest = onCloseDialog,
        onBookmarkNameChosen = onNewBookmarkNameChosen,
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

internal enum class DragAnchors {
  Start,
  End,
}

@Composable
internal fun BookmarkItem(
  bookmark: BookmarkItemViewState,
  onDelete: (Bookmark.Id) -> Unit,
  onEdit: (Bookmark.Id) -> Unit,
  onClick: (Bookmark.Id) -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current

  val deleteIconSize = 72.dp

  var expanded by remember { mutableStateOf(false) }

  val state = remember {
    AnchoredDraggableState(
      initialValue = DragAnchors.Start,
      positionalThreshold = { distance: Float -> distance },
      velocityThreshold = { with(density) { 100.dp.toPx() } },
      animationSpec = tween(),
      confirmValueChange = {
        if (it == DragAnchors.End) {
          onDelete(bookmark.id)
        }
        true
      },
    ).apply {
      updateAnchors(
        DraggableAnchors {
          DragAnchors.Start at 0f
          DragAnchors.End at with(density) { deleteIconSize.toPx() }
        },
      )
    }
  }
  Box(modifier) {
    Box(
      Modifier
        .size(deleteIconSize)
        .background(Color.Red),
    ) {
      Icon(
        modifier = Modifier.align(Alignment.Center),
        imageVector = Icons.Outlined.Delete,
        contentDescription = stringResource(id = StringsR.string.delete),
        tint = Color.White,
      )
    }
    ListItem(
      modifier = Modifier
        .offset {
          IntOffset(
            x = state
              .requireOffset()
              .roundToInt(),
            y = 0,
          )
        }
        .anchoredDraggable(state, Orientation.Horizontal)
        .clickable {
          onClick(bookmark.id)
        },
      headlineContent = {
        Text(text = bookmark.title)
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
  }
}

@Composable
@Preview
private fun BookmarkItemPreview() {
  BookmarkItem(
    bookmark = BookmarkItemViewState(
      title = "Bookmark 1",
      subtitle = "10:10:10 / 12:12:12",
      id = Bookmark.Id(UUID.randomUUID()),
    ),
    onDelete = {},
    onEdit = { },
    onClick = {},
  )
}
