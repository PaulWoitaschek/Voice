package voice.bookOverview.views

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import voice.bookOverview.R
import voice.bookOverview.bottomSheet.BottomSheetContent
import voice.bookOverview.bottomSheet.BottomSheetItem
import voice.bookOverview.deleteBook.DeleteBookDialog
import voice.bookOverview.di.BookOverviewComponent
import voice.bookOverview.editTitle.EditBookTitleDialog
import voice.bookOverview.overview.BookOverviewCategory
import voice.bookOverview.overview.BookOverviewItemViewState
import voice.bookOverview.overview.BookOverviewLayoutMode
import voice.bookOverview.overview.BookOverviewViewState
import voice.common.BookId
import voice.common.compose.VoiceTheme
import voice.common.compose.rememberScoped
import voice.common.rootComponentAs
import java.util.UUID

@Composable
fun BookOverviewScreen(modifier: Modifier = Modifier) {
  val bookComponent = rememberScoped {
    rootComponentAs<BookOverviewComponent.Factory.Provider>()
      .bookOverviewComponentProviderFactory.create()
  }
  val bookOverviewViewModel = bookComponent.bookOverviewViewModel
  val editBookTitleViewModel = bookComponent.editBookTitleViewModel
  val bottomSheetViewModel = bookComponent.bottomSheetViewModel
  val deleteBookViewModel = bookComponent.deleteBookViewModel
  val fileCoverViewModel = bookComponent.fileCoverViewModel
  LaunchedEffect(Unit) {
    bookOverviewViewModel.attach()
  }
  val viewState = bookOverviewViewModel.state()

  val scope = rememberCoroutineScope()

  val getContentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent(),
    onResult = { uri ->
      if (uri != null) {
        fileCoverViewModel.onImagePicked(uri)
      }
    },
  )

  val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
  ModalBottomSheetLayout(
    modifier = modifier,
    sheetState = bottomSheetState,
    sheetContent = {
      Surface {
        BottomSheetContent(bottomSheetViewModel.state.value) { item ->
          if (item == BottomSheetItem.FileCover) {
            getContentLauncher.launch("image/*")
          }
          scope.launch {
            delay(300)
            bottomSheetState.hide()
            bottomSheetViewModel.onItemClick(item)
          }
        }
      }
    },
  ) {
    BookOverview(
      viewState = viewState,
      onSettingsClick = bookOverviewViewModel::onSettingsClick,
      onBookClick = bookOverviewViewModel::onBookClick,
      onBookLongClick = { bookId ->
        scope.launch {
          bottomSheetViewModel.bookSelected(bookId)
          bottomSheetState.show()
        }
      },
      onBookFolderClick = bookOverviewViewModel::onBookFolderClick,
      onPlayButtonClick = bookOverviewViewModel::playPause,
      onBookMigrationClick = {
        bookOverviewViewModel.onBoomMigrationHelperConfirmClick()
        bookOverviewViewModel.onBookMigrationClick()
      },
      onBoomMigrationHelperConfirmClick = bookOverviewViewModel::onBoomMigrationHelperConfirmClick,
      onSearchClick = bookOverviewViewModel::onSearchClick,
    )
    val deleteBookViewState = deleteBookViewModel.state.value
    if (deleteBookViewState != null) {
      DeleteBookDialog(
        viewState = deleteBookViewState,
        onDismiss = deleteBookViewModel::onDismiss,
        onConfirmDeletion = deleteBookViewModel::onConfirmDeletion,
        onDeleteCheckBoxChecked = deleteBookViewModel::onDeleteCheckBoxChecked,
      )
    }
    val editBookTitleState = editBookTitleViewModel.state.value
    if (editBookTitleState != null) {
      EditBookTitleDialog(
        onDismissEditTitleClick = editBookTitleViewModel::onDismissEditTitle,
        onConfirmEditTitle = editBookTitleViewModel::onConfirmEditTitle,
        viewState = editBookTitleState,
        onUpdateEditTitle = editBookTitleViewModel::onUpdateEditTitle,
      )
    }
  }
}

@Composable
internal fun BookOverview(
  viewState: BookOverviewViewState,
  onSettingsClick: () -> Unit,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  onBookFolderClick: () -> Unit,
  onPlayButtonClick: () -> Unit,
  onBookMigrationClick: () -> Unit,
  onBoomMigrationHelperConfirmClick: () -> Unit,
  onSearchClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        title = {
          Text(text = stringResource(id = R.string.app_name))
        },
        scrollBehavior = scrollBehavior,
        actions = {
          if (viewState.showSearchIcon) {
            IconButton(onClick = onSearchClick) {
              Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
            }
          }
          if (viewState.showMigrateIcon) {
            MigrateIcon(
              onClick = onBookMigrationClick,
              withHint = viewState.showMigrateHint,
              onHintClick = onBoomMigrationHelperConfirmClick,
            )
          }
          BookFolderIcon(withHint = viewState.showAddBookHint, onClick = onBookFolderClick)

          SettingsIcon(onSettingsClick)
        },
      )
    },
    floatingActionButton = {
      if (viewState.playButtonState != null) {
        PlayButton(
          playing = viewState.playButtonState == BookOverviewViewState.PlayButtonState.Playing,
          onClick = onPlayButtonClick,
        )
      }
    },
  ) { contentPadding ->
    when (viewState) {
      is BookOverviewViewState.Content -> {
        when (viewState.layoutMode) {
          BookOverviewLayoutMode.List -> {
            ListBooks(
              books = viewState.books,
              onBookClick = onBookClick,
              onBookLongClick = onBookLongClick,
              contentPadding = contentPadding,
            )
          }
          BookOverviewLayoutMode.Grid -> {
            GridBooks(
              books = viewState.books,
              onBookClick = onBookClick,
              onBookLongClick = onBookLongClick,
              contentPadding = contentPadding,
            )
          }
        }
      }
      BookOverviewViewState.Loading -> {
        Box(
          Modifier
            .fillMaxSize()
            .padding(contentPadding),
        ) {
          CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
      }
    }
  }
}

@Preview // ktlint-disable twitter-compose:preview-public-check
@Composable
fun BookOverviewPreview(
  @PreviewParameter(BookOverviewPreviewParameterProvider::class)
  viewState: BookOverviewViewState,
) {
  VoiceTheme {
    BookOverview(
      viewState = viewState,
      onSettingsClick = {},
      onBookClick = {},
      onBookLongClick = {},
      onBookFolderClick = {},
      onPlayButtonClick = {},
      onBookMigrationClick = {},
      onBoomMigrationHelperConfirmClick = {},
      onSearchClick = {},
    )
  }
}

internal class BookOverviewPreviewParameterProvider : PreviewParameterProvider<BookOverviewViewState> {

  fun book(): BookOverviewItemViewState {
    return BookOverviewItemViewState(
      name = "Book",
      author = "Author",
      cover = null,
      progress = 0.8F,
      id = BookId(UUID.randomUUID().toString()),
      remainingTime = "01:04",
    )
  }

  override val values = sequenceOf(
    BookOverviewViewState.Loading,
    BookOverviewViewState.Content(
      books = mapOf(
        BookOverviewCategory.CURRENT to buildList { repeat(10) { add(book()) } },
        BookOverviewCategory.FINISHED to listOf(book(), book()),
      ),
      layoutMode = BookOverviewLayoutMode.List,
      playButtonState = BookOverviewViewState.PlayButtonState.Paused,
      showAddBookHint = false,
      showMigrateHint = false,
      showMigrateIcon = true,
      showSearchIcon = true,
    ),
  )
}
