package voice.features.bookOverview.views

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import voice.core.ui.CoverTheme
import voice.features.bookOverview.bottomSheet.BookActionsContent
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.deleteBook.DeleteBookSheet
import voice.features.bookOverview.di.BookOverviewGraph
import voice.features.bookOverview.editTitle.RenameBookSheet

/**
 * The menu a long press on a book opens, and the sheets its items lead to. The library and the search share it.
 *
 * Select the book on the graph's bottom sheet view model before showing the menu.
 */
@Composable
internal fun BookActions(
  bookGraph: BookOverviewGraph,
  showBottomSheet: Boolean,
  onBottomSheetDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val editBookTitleViewModel = bookGraph.editBookTitleViewModel
  val bottomSheetViewModel = bookGraph.bottomSheetViewModel
  val deleteBookViewModel = bookGraph.deleteBookViewModel
  val fileCoverViewModel = bookGraph.fileCoverViewModel

  val scope = rememberCoroutineScope()
  val getContentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent(),
    onResult = { uri ->
      if (uri != null) {
        fileCoverViewModel.onImagePicked(uri)
      }
    },
  )

  val deleteBookViewState = deleteBookViewModel.state.value
  if (deleteBookViewState != null) {
    CoverTheme(cover = deleteBookViewState.cover) {
      DeleteBookSheet(
        viewState = deleteBookViewState,
        onDismiss = deleteBookViewModel::onDismiss,
        onConfirmDeletion = deleteBookViewModel::onConfirmDeletion,
        onDeleteCheckBoxCheck = deleteBookViewModel::onDeleteCheckBoxCheck,
      )
    }
  }
  val editBookTitleState = editBookTitleViewModel.state.value
  if (editBookTitleState != null) {
    CoverTheme(cover = editBookTitleState.cover) {
      RenameBookSheet(
        viewState = editBookTitleState,
        onTitleChange = editBookTitleViewModel::onUpdateEditTitle,
        onConfirm = editBookTitleViewModel::onConfirmEditTitle,
        onDismiss = editBookTitleViewModel::onDismissEditTitle,
      )
    }
  }

  val state = bottomSheetViewModel.state.value
  val book = state.book
  // shown once the book is loaded, so the sheet rises at its full height
  if (showBottomSheet && book != null) {
    CoverTheme(cover = book.cover) {
      val sheetState = rememberBottomSheetState(
        initialValue = Hidden,
        enabledValues = setOf(Hidden, Expanded),
      )
      var pendingStatusChange by remember { mutableStateOf<Job?>(null) }
      // the first choice wins, so taps while the sheet slides away can't add a second action
      var closing by remember { mutableStateOf(false) }
      fun hideThen(item: BottomSheetItem) {
        if (closing) return
        closing = true
        scope.launch {
          sheetState.hide()
          bottomSheetViewModel.onItemClick(item)
          onBottomSheetDismiss()
        }
      }
      ModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = {
          pendingStatusChange?.cancel()
          onBottomSheetDismiss()
        },
      ) {
        BookActionsContent(
          book = book,
          category = state.category,
          items = state.items,
          onItemClick = { item ->
            if (!closing) {
              if (item == BottomSheetItem.FileCover) {
                getContentLauncher.launch("image/*")
              }
              hideThen(item)
            }
          },
          onStatusChange = { item ->
            pendingStatusChange?.cancel()
            // the book's own status isn't an item, so picking it again only cancels the change
            if (item in state.items) {
              pendingStatusChange = scope.launch {
                // a moment to see the new status take shape before the sheet goes
                delay(450)
                hideThen(item)
              }
            }
          },
        )
      }
    }
  }
}
