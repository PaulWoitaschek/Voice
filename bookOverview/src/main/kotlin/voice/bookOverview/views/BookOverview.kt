package voice.bookOverview.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.squareup.anvil.annotations.ContributesTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import voice.bookOverview.R
import voice.bookOverview.editTitle.EditBookTitleViewModel
import voice.bookOverview.overview.BookOverviewCategory
import voice.bookOverview.overview.BookOverviewViewModel
import voice.bookOverview.overview.BookOverviewViewState
import voice.common.AppScope
import voice.common.compose.VoiceTheme
import voice.common.rootComponentAs
import voice.data.Book
import java.util.UUID

@ContributesTo(AppScope::class)
interface BookOverviewComponent {
  val bookOverviewViewModel: BookOverviewViewModel
  val editBookTitleViewModel: EditBookTitleViewModel
}

@Composable
fun BookOverviewScreen(
  onSettingsClick: () -> Unit,
  onBookMigrationClick: () -> Unit,
  toFolderOverview: () -> Unit,
  toBook: (Book.Id) -> Unit,
  onCoverFromInternetClick: (Book.Id) -> Unit,
  onFileCoverClick: (Book.Id) -> Unit,
) {
  val bookComponent = remember { rootComponentAs<BookOverviewComponent>() }
  val bookOverviewViewModel = remember {
    bookComponent.bookOverviewViewModel
  }
  val editBookTitleViewModel = remember {
    bookComponent.editBookTitleViewModel
  }
  LaunchedEffect(Unit) {
    bookOverviewViewModel.attach()
  }
  val lifecycleOwner = LocalLifecycleOwner.current
  val viewState by remember(lifecycleOwner, bookOverviewViewModel) {
    bookOverviewViewModel.state().flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.RESUMED)
  }.collectAsState(initial = BookOverviewViewState.Loading)

  val scope = rememberCoroutineScope()

  var editingBook by remember { mutableStateOf<Book.Id?>(null) }
  val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
  ModalBottomSheetLayout(
    sheetState = bottomSheetState,
    sheetContent = {
      BottomSheetContent { item ->
        scope.launch {
          delay(300)
          bottomSheetState.hide()
          when (item) {
            BottomSheetItem.Title -> {
              editingBook?.let(editBookTitleViewModel::onEditBookTitleClick)
            }
            BottomSheetItem.InternetCover -> editingBook?.let(onCoverFromInternetClick)
            BottomSheetItem.FileCover -> editingBook?.let(onFileCoverClick)
          }
        }
      }
    }
  ) {
    BookOverview(
      viewState = viewState,
      onLayoutIconClick = bookOverviewViewModel::toggleGrid,
      onSettingsClick = onSettingsClick,
      onBookClick = toBook,
      onBookLongClick = { bookId ->
        scope.launch {
          editingBook = bookId
          bottomSheetState.show()
        }
      },
      onBookFolderClick = toFolderOverview,
      onPlayButtonClick = bookOverviewViewModel::playPause,
      onBookMigrationClick = {
        bookOverviewViewModel.onBoomMigrationHelperConfirmClick()
        onBookMigrationClick()
      },
      onBoomMigrationHelperConfirmClick = bookOverviewViewModel::onBoomMigrationHelperConfirmClick,
    )
    val editBookTitleState = editBookTitleViewModel.state.value
    if (editBookTitleState != null) {
      EditBookTitleDialog(
        onDismissEditTitleClick = editBookTitleViewModel::onDismissEditTitle,
        onConfirmEditTitle = editBookTitleViewModel::onConfirmEditTitle,
        editBookTitleState = editBookTitleState,
        onUpdateEditTitle = editBookTitleViewModel::onUpdateEditTitle
      )
    }
  }
}

@Composable
internal fun BookOverview(
  viewState: BookOverviewViewState,
  onLayoutIconClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onBookClick: (Book.Id) -> Unit,
  onBookLongClick: (Book.Id) -> Unit,
  onBookFolderClick: () -> Unit,
  onPlayButtonClick: () -> Unit,
  onBookMigrationClick: () -> Unit,
  onBoomMigrationHelperConfirmClick: () -> Unit,
) {
  Scaffold(
    topBar = {
      SmallTopAppBar(
        title = {
          Text(text = stringResource(id = R.string.app_name))
        },
        actions = {
          if (viewState.showMigrateIcon) {
            MigrateIcon(
              onClick = onBookMigrationClick,
              withHint = viewState.showMigrateHint,
              onHintClick = onBoomMigrationHelperConfirmClick
            )
          }
          BookFolderIcon(withHint = viewState.showAddBookHint, onClick = onBookFolderClick)

          val layoutIcon = viewState.layoutIcon
          if (layoutIcon != null) {
            LayoutIcon(layoutIcon, onLayoutIconClick)
          }
          SettingsIcon(onSettingsClick)
        }
      )
    },
    floatingActionButton = {
      if (viewState.playButtonState != null) {
        PlayButton(
          playing = viewState.playButtonState == BookOverviewViewState.PlayButtonState.Playing,
          onClick = onPlayButtonClick
        )
      }
    }
  ) { contentPadding ->
    when (viewState) {
      is BookOverviewViewState.Content -> {
        when (viewState.layoutMode) {
          BookOverviewViewState.Content.LayoutMode.List -> {
            ListBooks(
              books = viewState.books,
              onBookClick = onBookClick,
              onBookLongClick = onBookLongClick,
              contentPadding = contentPadding,
            )
          }
          BookOverviewViewState.Content.LayoutMode.Grid -> {
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
            .padding(contentPadding)
        ) {
          CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
      }
    }
  }
}


@Preview
@Composable
private fun BookOverviewPreview(
  @PreviewParameter(BookOverviewPreviewParameterProvider::class)
  viewState: BookOverviewViewState
) {
  VoiceTheme {
    BookOverview(
      viewState = viewState,
      onLayoutIconClick = {},
      onSettingsClick = {},
      onBookClick = {},
      onBookLongClick = {},
      onBookFolderClick = {},
      onPlayButtonClick = {},
      onBookMigrationClick = {},
    ) {}
  }
}

internal class BookOverviewPreviewParameterProvider : PreviewParameterProvider<BookOverviewViewState> {

  fun book(): BookOverviewViewState.Content.BookViewState {
    return BookOverviewViewState.Content.BookViewState(
      name = "Book",
      author = "Author",
      cover = null,
      progress = 0.8F,
      id = Book.Id(UUID.randomUUID().toString()),
      remainingTime = "01:04"
    )
  }

  override val values = sequenceOf(
    BookOverviewViewState.Loading,
    BookOverviewViewState.Content(
      layoutIcon = BookOverviewViewState.Content.LayoutIcon.List,
      books = mapOf(
        BookOverviewCategory.CURRENT to buildList { repeat(10) { add(book()) } },
        BookOverviewCategory.FINISHED to listOf(book(), book()),
      ),
      layoutMode = BookOverviewViewState.Content.LayoutMode.List,
      playButtonState = BookOverviewViewState.PlayButtonState.Paused,
      showAddBookHint = false,
      showMigrateHint = false,
      showMigrateIcon = true,
    )
  )
}
