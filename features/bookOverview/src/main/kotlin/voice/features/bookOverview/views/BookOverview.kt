@file:OptIn(ExperimentalMaterial3Api::class)

package voice.features.bookOverview.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.ui.PlayButton
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.features.bookOverview.di.BookOverviewGraph
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.features.bookOverview.overview.BookOverviewItemViewState
import voice.features.bookOverview.overview.BookOverviewLayoutMode
import voice.features.bookOverview.overview.BookOverviewViewState
import voice.features.bookOverview.overview.LibraryItem
import voice.features.bookOverview.overview.SeriesStackViewState
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import kotlin.uuid.Uuid
import voice.core.strings.R as StringsR

@BindingContainer
@ContributesTo(AppScope::class)
object BookOverviewProvider {

  @Provides
  @IntoSet
  fun bookOverviewNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.BookOverview> { key ->
    NavEntry(key) {
      BookOverviewScreen()
    }
  }
}

@Composable
fun BookOverviewScreen(modifier: Modifier = Modifier) {
  val bookGraph = retain<BookOverviewGraph> {
    rootGraphAs<BookOverviewGraph.Factory.Provider>()
      .bookOverviewGraphProviderFactory.create()
  }
  val bookOverviewViewModel = bookGraph.bookOverviewViewModel

  LaunchedEffect(Unit) {
    bookOverviewViewModel.attach()
  }
  val viewState = bookOverviewViewModel.state()
  val snackbarHostState = remember { SnackbarHostState() }
  val resources = LocalResources.current
  val undoLabel = stringResource(StringsR.string.common_action_undo)
  LaunchedEffect(bookOverviewViewModel) {
    bookOverviewViewModel.hiddenBooks.collect { book ->
      val result = snackbarHostState.showSnackbar(
        message = resources.getString(StringsR.string.library_book_hidden_message, book.name),
        actionLabel = undoLabel,
        duration = SnackbarDuration.Short,
      )
      if (result == SnackbarResult.ActionPerformed) {
        bookOverviewViewModel.onUndoHide(book.id)
      }
    }
  }

  BookOverview(
    viewState = viewState,
    onSettingsClick = bookOverviewViewModel::onSettingsClick,
    onBookClick = bookOverviewViewModel::onBookClick,
    onBookLongClick = bookOverviewViewModel::onBookLongClick,
    onSeriesClick = bookOverviewViewModel::onSeriesClick,
    onBookFolderClick = bookOverviewViewModel::onBookFolderClick,
    onFolderPickerMovedDialogDismiss = bookOverviewViewModel::onFolderPickerMovedDialogDismiss,
    onPlayButtonClick = bookOverviewViewModel::playPause,
    onSearchClick = bookOverviewViewModel::onSearchClick,
    onPermissionBugCardClick = bookOverviewViewModel::onPermissionBugCardClick,
    onSurveyClick = bookOverviewViewModel::onSurveyClick,
    onSurveyDismiss = bookOverviewViewModel::onSurveyDismiss,
    onShowFoldersClick = bookOverviewViewModel::onShowFoldersClick,
    onShowHiddenBooksChange = bookOverviewViewModel::onShowHiddenBooksChange,
    snackbarHostState = snackbarHostState,
    modifier = modifier,
  )
}

@Composable
internal fun BookOverview(
  viewState: BookOverviewViewState,
  onSettingsClick: () -> Unit,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  onSeriesClick: (String) -> Unit,
  onBookFolderClick: () -> Unit,
  onFolderPickerMovedDialogDismiss: () -> Unit,
  onPlayButtonClick: () -> Unit,
  onSearchClick: () -> Unit,
  onPermissionBugCardClick: () -> Unit,
  onSurveyClick: () -> Unit,
  onSurveyDismiss: () -> Unit,
  onShowFoldersClick: () -> Unit,
  onShowHiddenBooksChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
  val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  val gridState = rememberLazyGridState()
  var showFilterSheet by rememberSaveable { mutableStateOf(false) }
  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      LibraryTopBar(
        showSearch = viewState.showSearchIcon,
        showFolderPickerIcon = viewState.showFolderPickerIcon,
        showFolderAccessBadge = viewState.showFolderAccessBadge,
        // only there once a book is hidden, so a library without any stays as it was
        showFilter = viewState.hiddenBookCount > 0 || viewState.showHiddenBooks,
        isLoading = viewState.isLoading,
        scrollBehavior = scrollBehavior,
        onSearchClick = onSearchClick,
        onBookFolderClick = onBookFolderClick,
        onFilterClick = { showFilterSheet = true },
        onSettingsClick = onSettingsClick,
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    floatingActionButton = {
      val playButtonState = viewState.playButtonState
      AnimatedVisibility(
        visible = playButtonState != null && !gridState.heroPlayButtonVisible(),
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
      ) {
        PlayButton(
          playing = playButtonState == BookOverviewViewState.PlayButtonState.Playing,
          onClick = onPlayButtonClick,
          modifier = Modifier.navigationBarsPadding(),
          size = 64.dp,
        )
      }
    },
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
  ) { contentPadding ->
    Library(
      viewState = viewState,
      gridState = gridState,
      onBookClick = onBookClick,
      onBookLongClick = onBookLongClick,
      onSeriesClick = onSeriesClick,
      onPlayClick = onPlayButtonClick,
      onPermissionBugCardClick = onPermissionBugCardClick,
      onSurveyClick = onSurveyClick,
      onSurveyDismiss = onSurveyDismiss,
      onShowFoldersClick = onShowFoldersClick,
      onHiddenBooksClick = { showFilterSheet = true },
      modifier = Modifier
        .padding(contentPadding)
        .consumeWindowInsets(contentPadding),
    )
  }
  Dialog(
    dialog = viewState.dialog,
    onFolderPickerMovedDialogDismiss = onFolderPickerMovedDialogDismiss,
  )
  if (showFilterSheet) {
    LibraryFilterSheet(
      hiddenBookCount = viewState.hiddenBookCount,
      showHiddenBooks = viewState.showHiddenBooks,
      onShowHiddenBooksChange = onShowHiddenBooksChange,
      onDismiss = { showFilterSheet = false },
    )
  }
}

/** Whether the hero card's play button is on screen, which makes a floating one redundant. */
@Composable
private fun LazyGridState.heroPlayButtonVisible(): Boolean {
  // the play button sits at the bottom of the card
  val playButtonInset = with(LocalDensity.current) { 56.dp.roundToPx() }
  val visible by remember(this, playButtonInset) {
    derivedStateOf {
      val hero = layoutInfo.visibleItemsInfo.firstOrNull { it.key == HERO_KEY }
      hero != null && hero.offset.y + hero.size.height - playButtonInset > layoutInfo.viewportStartOffset
    }
  }
  return visible
}

@Composable
private fun Dialog(
  dialog: BookOverviewViewState.Dialog?,
  onFolderPickerMovedDialogDismiss: () -> Unit,
) {
  when (dialog) {
    BookOverviewViewState.Dialog.FolderPickerMovedToSettings -> {
      AlertDialog(
        onDismissRequest = onFolderPickerMovedDialogDismiss,
        icon = {
          Row {
            Icon(imageVector = VoiceIcons.ArrowForward, contentDescription = null)
            Icon(imageVector = VoiceIcons.Settings, contentDescription = null)
            Icon(imageVector = VoiceIcons.ArrowBack, contentDescription = null)
          }
        },
        title = {
          Text(stringResource(StringsR.string.library_folders_moved_dialog_title))
        },
        text = {
          Text(stringResource(StringsR.string.library_folders_moved_dialog_message))
        },
        confirmButton = {
          TextButton(onClick = onFolderPickerMovedDialogDismiss) {
            Text(stringResource(StringsR.string.common_dialog_ok))
          }
        },
      )
    }
    null -> Unit
  }
}

@Suppress("ktlint:compose:preview-public-check")
@Preview
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
      onSeriesClick = {},
      onBookFolderClick = {},
      onFolderPickerMovedDialogDismiss = {},
      onPlayButtonClick = {},
      onSearchClick = {},
      onPermissionBugCardClick = {},
      onSurveyClick = {},
      onSurveyDismiss = {},
      onShowFoldersClick = {},
      onShowHiddenBooksChange = {},
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
      id = BookId(Uuid.random().toString()),
      remainingTime = "01:04",
    )
  }

  private val library = BookOverviewViewState(
    currentBookId = null,
    sections = mapOf(
      BookOverviewCategory.CURRENT to buildList {
        add(
          LibraryItem.Series(
            SeriesStackViewState(
              key = "signalsaga",
              name = "The Signal Saga",
              author = "Author",
              covers = listOf(null, null, null),
              bookCount = 4,
              finishedCount = 1,
              progress = 0.4F,
              category = BookOverviewCategory.CURRENT,
            ),
          ),
        )
        repeat(9) {
          val book = book()
          add(LibraryItem.Book(book.id, mutableStateOf(book)))
        }
      },
      BookOverviewCategory.FINISHED to List(2) {
        val book = book()
        LibraryItem.Book(book.id, mutableStateOf(book))
      },
    ),
    hero = mutableStateOf(book().copy(series = "The Signal Saga 2")),
    inProgressCount = 10,
    layoutMode = BookOverviewLayoutMode.List,
    playButtonState = BookOverviewViewState.PlayButtonState.Paused,
    showEmptyLibrary = false,
    showSearchIcon = true,
    isLoading = true,
    showStoragePermissionBugCard = false,
    showSurveyCard = false,
    showFolderAccessBadge = false,
    showFolderPickerIcon = true,
    dialog = null,
  )

  override val values = sequenceOf(
    library,
    library.copy(layoutMode = BookOverviewLayoutMode.Grid, showSurveyCard = true),
    BookOverviewViewState.Loading.copy(
      isLoading = false,
      showEmptyLibrary = true,
      showFolderAccessBadge = true,
      showFolderPickerIcon = false,
    ),
    library.copy(
      sections = emptyMap(),
      hero = null,
      inProgressCount = 0,
      playButtonState = null,
      hiddenBookCount = 3,
    ),
  )
}
