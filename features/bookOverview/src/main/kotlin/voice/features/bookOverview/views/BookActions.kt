package voice.features.bookOverview.views

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.ui.CoverTheme
import voice.features.bookOverview.bottomSheet.BookActionsContent
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.markAsItem
import voice.features.bookOverview.deleteBook.DeleteBookSheet
import voice.features.bookOverview.di.BookActionsGraph
import voice.features.bookOverview.editTitle.RenameBookSheet
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.navigation.OverlayNav

@BindingContainer
@ContributesTo(AppScope::class)
object BookActionsProvider {

  @Provides
  @IntoSet
  fun bookActionsNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.BookActions> { key ->
    NavEntry(key, metadata = OverlayNav.overlay()) {
      BookActionsScreen(key.bookId)
    }
  }
}

@Composable
private fun BookActionsScreen(bookId: BookId) {
  val bookGraph = retain<BookActionsGraph>(bookId) {
    rootGraphAs<BookActionsGraph.Factory.Provider>()
      .bookActionsGraphProviderFactory.createBookActionsGraph(bookId)
  }
  BookActions(bookId, bookGraph)
}

/**
 * The menu of a book and the sheets its items lead to. It closes once its last sheet is gone, so it is still around to
 * show the rename and delete sheets and to receive the picked cover.
 */
@Composable
private fun BookActions(
  bookId: BookId,
  bookGraph: BookActionsGraph,
) {
  val editBookTitleViewModel = bookGraph.editBookTitleViewModel
  val bottomSheetViewModel = bookGraph.bottomSheetViewModel
  val deleteBookViewModel = bookGraph.deleteBookViewModel
  val fileCoverViewModel = bookGraph.fileCoverViewModel

  val scope = rememberCoroutineScope()
  var showMenu by rememberSaveable { mutableStateOf(true) }
  var pickingCover by rememberSaveable { mutableStateOf(false) }
  fun closeIfDone() {
    if (!showMenu &&
      !pickingCover &&
      editBookTitleViewModel.state.value == null &&
      deleteBookViewModel.state.value == null
    ) {
      bottomSheetViewModel.onClose()
    }
  }
  // a recreated activity restores the closed menu, but not the sheets that were open after it
  LaunchedEffect(Unit) {
    closeIfDone()
  }

  val getContentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent(),
    onResult = { uri ->
      pickingCover = false
      if (uri != null) {
        fileCoverViewModel.onImagePicked(bookId, uri)
      }
      closeIfDone()
    },
  )

  // the download shows its progress in a notification, which needs the permission since Android 13
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = {},
  )
  val context = LocalContext.current

  val deleteBookViewState = deleteBookViewModel.state.value
  if (deleteBookViewState != null) {
    CoverTheme(cover = deleteBookViewState.cover) {
      DeleteBookSheet(
        viewState = deleteBookViewState,
        onDismiss = {
          deleteBookViewModel.onDismiss()
          closeIfDone()
        },
        onConfirmDeletion = {
          deleteBookViewModel.onConfirmDeletion()
          closeIfDone()
        },
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
        onConfirm = {
          editBookTitleViewModel.onConfirmEditTitle()
          closeIfDone()
        },
        onDismiss = {
          editBookTitleViewModel.onDismissEditTitle()
          closeIfDone()
        },
      )
    }
  }

  val state = bottomSheetViewModel.state()
  // shown once the book is loaded, so the sheet rises at its full height
  if (showMenu && state != null) {
    CoverTheme(cover = state.book.cover) {
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
          showMenu = false
          closeIfDone()
        }
      }
      ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
          if (!closing) {
            pendingStatusChange?.cancel()
            showMenu = false
            closeIfDone()
          }
        },
      ) {
        BookActionsContent(
          book = state.book,
          source = state.source,
          download = state.download,
          bookSize = state.bookSize,
          category = state.category,
          items = state.items,
          onItemClick = { item ->
            if (!closing) {
              if (item == BottomSheetItem.FileCover) {
                pickingCover = true
                getContentLauncher.launch("image/*")
              }
              if (item == BottomSheetItem.Download && state.askForNotifications && !context.canPostNotifications()) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                bottomSheetViewModel.onAskedForNotifications()
              }
              hideThen(item)
            }
          },
          onStatusChange = { category ->
            pendingStatusChange?.cancel()
            // picking the book's own status again only cancels the change
            if (category != state.category) {
              pendingStatusChange = scope.launch {
                // a moment to see the new status take shape before the sheet goes
                delay(450)
                hideThen(category.markAsItem())
              }
            }
          },
        )
      }
    }
  }
}

private fun Context.canPostNotifications(): Boolean {
  return Build.VERSION.SDK_INT < 33 ||
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
