@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlin.uuid.Uuid
import voice.core.common.rootGraphAs
import voice.core.data.BookCategory
import voice.core.data.BookId
import voice.core.strings.R as StringsR
import voice.core.ui.PlayButton
import voice.core.ui.R as UiR
import voice.core.ui.VoiceTheme
import voice.features.bookOverview.di.BookOverviewGraph
import voice.features.bookOverview.overview.AuthorEntryViewState
import voice.features.bookOverview.overview.BookOverviewItemViewState
import voice.features.bookOverview.overview.BookOverviewLayoutMode
import voice.features.bookOverview.overview.BookOverviewViewState
import voice.features.bookOverview.overview.LibraryGrouping
import voice.features.bookOverview.overview.LibraryItem
import voice.features.bookOverview.overview.LibrarySource
import voice.features.bookOverview.overview.SeriesStackViewState
import voice.features.bookOverview.select.BookField
import voice.features.bookOverview.select.SelectionEditSheet
import voice.features.bookOverview.select.SelectionToolbar
import voice.features.bookOverview.select.SelectionTopBar
import voice.features.bookOverview.select.SelectionViewState
import voice.navigation.Destination
import voice.navigation.NavEntryProvider

@BindingContainer
@ContributesTo(AppScope::class)
object BookOverviewProvider {

  @Provides
  @IntoSet
  fun bookOverviewNavEntryProvider(): NavEntryProvider<*> =
    NavEntryProvider<Destination.BookOverview> { key ->
      NavEntry(key) {
        BookOverviewScreen()
      }
    }
}

@Composable
fun BookOverviewScreen(modifier: Modifier = Modifier) {
  val bookGraph =
    retain<BookOverviewGraph> {
      rootGraphAs<BookOverviewGraph.Factory.Provider>().bookOverviewGraphProviderFactory.create()
    }
  val bookOverviewViewModel = bookGraph.bookOverviewViewModel

  // Voice stays alive in the background, so books copied meanwhile are found when it comes back
  LifecycleEventEffect(Lifecycle.Event.ON_START) {
    bookOverviewViewModel.attach()
  }
  val viewState = bookOverviewViewModel.state()
  val snackbarHostState = remember { SnackbarHostState() }
  val resources = LocalResources.current
  val undoLabel = stringResource(StringsR.string.common_action_undo)
  LaunchedEffect(bookOverviewViewModel) {
    bookOverviewViewModel.hiddenBooks.collect { hidden ->
      val count = hidden.ids.size
      val message =
        when {
          hidden.name == null ->
            resources.getQuantityString(StringsR.plurals.library_selection_hidden, count, count)
          hidden.series ->
            resources.getString(StringsR.string.library_series_hidden_message, hidden.name)
          else -> resources.getString(StringsR.string.library_book_hidden_message, hidden.name)
        }
      val result =
        snackbarHostState.showSnackbar(
          message = message,
          actionLabel = undoLabel,
          duration = SnackbarDuration.Short,
        )
      if (result == SnackbarResult.ActionPerformed) {
        bookOverviewViewModel.onUndoHide(hidden.ids)
      }
    }
  }
  LaunchedEffect(bookOverviewViewModel) {
    bookOverviewViewModel.booksEdited.collect { edited ->
      val failed = edited.failed.size
      val message =
        when {
          edited.notAllowed -> resources.getString(StringsR.string.book_edit_server_not_allowed)
          failed > 0 ->
            resources.getQuantityString(
              StringsR.plurals.library_selection_server_failed,
              failed,
              failed,
            )
          else -> {
            val saved =
              when (edited.field) {
                BookField.Series -> StringsR.plurals.library_selection_series_saved
                BookField.Author -> StringsR.plurals.library_selection_author_saved
              }
            resources.getQuantityString(saved, edited.count, edited.count)
          }
        }
      val result =
        snackbarHostState.showSnackbar(
          message = message,
          // the books that were saved can still go back
          actionLabel = undoLabel.takeIf { edited.count > 0 },
          duration = if (failed > 0) SnackbarDuration.Long else SnackbarDuration.Short,
        )
      if (result == SnackbarResult.ActionPerformed) {
        bookOverviewViewModel.onUndoEdit(edited)
      }
    }
  }
  LaunchedEffect(bookOverviewViewModel) {
    bookOverviewViewModel.selectionRequests.collect(bookOverviewViewModel::onSelectionStart)
  }

  BookOverview(
    viewState = viewState,
    onSettingsClick = bookOverviewViewModel::onSettingsClick,
    onBookClick = bookOverviewViewModel::onBookClick,
    onBookLongClick = bookOverviewViewModel::onBookLongClick,
    onSeriesClick = bookOverviewViewModel::onSeriesClick,
    onSeriesLongClick = bookOverviewViewModel::onSeriesLongClick,
    onAuthorClick = bookOverviewViewModel::onAuthorClick,
    onAuthorLongClick = bookOverviewViewModel::onAuthorLongClick,
    onBookFolderClick = bookOverviewViewModel::onBookFolderClick,
    onFolderPickerMovedDialogDismiss = bookOverviewViewModel::onFolderPickerMovedDialogDismiss,
    onPlayButtonClick = bookOverviewViewModel::playPause,
    onSearchClick = bookOverviewViewModel::onSearchClick,
    onPermissionBugCardClick = bookOverviewViewModel::onPermissionBugCardClick,
    onSurveyClick = bookOverviewViewModel::onSurveyClick,
    onSurveyDismiss = bookOverviewViewModel::onSurveyDismiss,
    onShowFoldersClick = bookOverviewViewModel::onShowFoldersClick,
    onShowHiddenBooksChange = bookOverviewViewModel::onShowHiddenBooksChange,
    onGroupingChange = bookOverviewViewModel::onGroupingChange,
    onShowNotDownloadedBooksChange = bookOverviewViewModel::onShowNotDownloadedBooksChange,
    onSourceChange = bookOverviewViewModel::onSourceChange,
    onSelectionToggle = bookOverviewViewModel::onSelectionToggle,
    onSelectAll = bookOverviewViewModel::onSelectAll,
    onSelectionClose = bookOverviewViewModel::onSelectionClose,
    onSelectionEdit = bookOverviewViewModel::onSelectionEdit,
    onSelectionHide = bookOverviewViewModel::onSelectionHide,
    onSelectionShow = bookOverviewViewModel::onSelectionShow,
    snackbarHostState = snackbarHostState,
    modifier = modifier,
  )
  viewState.selectionEdit?.let { edit ->
    SelectionEditSheet(
      viewState = edit,
      onSave = bookOverviewViewModel::onSelectionEditSave,
      onDismiss = bookOverviewViewModel::onSelectionEditDismiss,
    )
  }
}

@Composable
internal fun BookOverview(
  viewState: BookOverviewViewState,
  onSettingsClick: () -> Unit,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  onSeriesClick: (String) -> Unit,
  onSeriesLongClick: (String) -> Unit,
  onAuthorClick: (String?) -> Unit,
  onAuthorLongClick: (AuthorEntryViewState) -> Unit,
  onBookFolderClick: () -> Unit,
  onFolderPickerMovedDialogDismiss: () -> Unit,
  onPlayButtonClick: () -> Unit,
  onSearchClick: () -> Unit,
  onPermissionBugCardClick: () -> Unit,
  onSurveyClick: () -> Unit,
  onSurveyDismiss: () -> Unit,
  onShowFoldersClick: () -> Unit,
  onShowHiddenBooksChange: (Boolean) -> Unit,
  onGroupingChange: (LibraryGrouping) -> Unit,
  onShowNotDownloadedBooksChange: (Boolean) -> Unit,
  onSourceChange: (LibrarySource, Boolean) -> Unit,
  onSelectionToggle: (List<BookId>) -> Unit,
  onSelectAll: () -> Unit,
  onSelectionClose: () -> Unit,
  onSelectionEdit: (BookField) -> Unit,
  onSelectionHide: () -> Unit,
  onSelectionShow: () -> Unit,
  modifier: Modifier = Modifier,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
  BackHandler(enabled = viewState.selection != null, onBack = onSelectionClose)
  val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  val gridState = rememberLazyGridState()
  var showFilterSheet by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(scrollBehavior, gridState) {
    // the bar adds up scroll deltas to tell whether the library is under it. They drift when books
    // above the visible ones change, which kept the bar tinted with the library at the top.
    snapshotFlow { gridState.canScrollBackward }
      .collect { scrolled ->
        if (!scrolled) scrollBehavior.scrollState.contentOffset = 0F
      }
  }
  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      // while selecting, a new count doesn't swap the bar
      AnimatedContent(
        targetState = viewState.selection,
        contentKey = { it != null },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
      ) { selection ->
        if (selection == null) {
          LibraryTopBar(
            showSearch = viewState.showSearchIcon,
            showFolderPickerIcon = viewState.showFolderPickerIcon,
            showFolderAccessBadge = viewState.showFolderAccessBadge,
            showFilter = viewState.showFilterIcon,
            isLoading = viewState.isLoading,
            scrollBehavior = scrollBehavior,
            onSearchClick = onSearchClick,
            onBookFolderClick = onBookFolderClick,
            onFilterClick = { showFilterSheet = true },
            onSettingsClick = onSettingsClick,
          )
        } else {
          SelectionTopBar(
            selection = selection,
            onClose = onSelectionClose,
            onSelectAll = onSelectAll,
          )
        }
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    floatingActionButton = {
      val playButtonState = viewState.playButtonState
      AnimatedVisibility(
        visible =
          playButtonState != null &&
            viewState.selection == null &&
            !gridState.heroPlayButtonVisible(),
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
    Box(Modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(contentPadding)) {
      Library(
        viewState = viewState,
        gridState = gridState,
        onBookClick = onBookClick,
        onBookLongClick = onBookLongClick,
        onSeriesClick = onSeriesClick,
        onSeriesLongClick = onSeriesLongClick,
        onAuthorClick = onAuthorClick,
        onAuthorLongClick = onAuthorLongClick,
        onPlayClick = onPlayButtonClick,
        onPermissionBugCardClick = onPermissionBugCardClick,
        onSurveyClick = onSurveyClick,
        onSurveyDismiss = onSurveyDismiss,
        onShowFoldersClick = onShowFoldersClick,
        onHiddenBooksClick = { showFilterSheet = true },
        onSelectionToggle = onSelectionToggle,
      )
      AnimatedContent(
        targetState = viewState.selection,
        contentKey = { it != null },
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = FloatingToolbarDefaults.ScreenOffset),
        transitionSpec = {
          (slideInVertically { it } + fadeIn()) togetherWith
            (slideOutVertically { it } + fadeOut()) using
            SizeTransform(clip = false)
        },
      ) { selection ->
        if (selection != null) {
          SelectionToolbar(
            allHidden = selection.allHidden,
            onEdit = onSelectionEdit,
            onHide = onSelectionHide,
            onShow = onSelectionShow,
          )
        }
      }
    }
  }
  Dialog(
    dialog = viewState.dialog,
    onFolderPickerMovedDialogDismiss = onFolderPickerMovedDialogDismiss,
  )
  if (showFilterSheet) {
    LibraryFilterSheet(
      viewState = viewState,
      onGroupingChange = onGroupingChange,
      onShowHiddenBooksChange = onShowHiddenBooksChange,
      onShowNotDownloadedBooksChange = onShowNotDownloadedBooksChange,
      onSourceChange = onSourceChange,
      onDismiss = { showFilterSheet = false },
    )
  }
}

/** Whether the hero card's play button is on screen, which makes a floating one redundant. */
@Composable
private fun LazyGridState.heroPlayButtonVisible(): Boolean {
  // the play button sits at the bottom of the card
  val playButtonInset = with(LocalDensity.current) { 56.dp.roundToPx() }
  val visible by
    remember(this, playButtonInset) {
      derivedStateOf {
        val hero = layoutInfo.visibleItemsInfo.firstOrNull { it.key == HERO_KEY }
        hero != null &&
          hero.offset.y + hero.size.height - playButtonInset > layoutInfo.viewportStartOffset
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
            Icon(
              painter = painterResource(UiR.drawable.ic_arrow_forward),
              contentDescription = null,
            )
            Icon(painter = painterResource(UiR.drawable.ic_settings), contentDescription = null)
            Icon(painter = painterResource(UiR.drawable.ic_arrow_back), contentDescription = null)
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

@Suppress("ComposePreviewPublic")
@Preview
@Composable
fun BookOverviewPreview(
  @PreviewParameter(BookOverviewPreviewParameterProvider::class) viewState: BookOverviewViewState,
) {
  VoiceTheme {
    BookOverview(
      viewState = viewState,
      onSettingsClick = {},
      onBookClick = {},
      onBookLongClick = {},
      onSeriesClick = {},
      onSeriesLongClick = {},
      onAuthorClick = {},
      onAuthorLongClick = {},
      onBookFolderClick = {},
      onFolderPickerMovedDialogDismiss = {},
      onPlayButtonClick = {},
      onSearchClick = {},
      onPermissionBugCardClick = {},
      onSurveyClick = {},
      onSurveyDismiss = {},
      onShowFoldersClick = {},
      onShowHiddenBooksChange = {},
      onGroupingChange = {},
      onShowNotDownloadedBooksChange = {},
      onSourceChange = { _, _ -> },
      onSelectionToggle = {},
      onSelectAll = {},
      onSelectionClose = {},
      onSelectionEdit = {},
      onSelectionHide = {},
      onSelectionShow = {},
    )
  }
}

internal class BookOverviewPreviewParameterProvider :
  PreviewParameterProvider<BookOverviewViewState> {

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

  private val library =
    BookOverviewViewState(
      currentBookId = null,
      sections =
        mapOf(
          BookCategory.CURRENT to
            buildList {
              add(
                LibraryItem.Series(
                  SeriesStackViewState(
                    key = "signalsaga",
                    name = "The Signal Saga",
                    author = "Author",
                    covers = listOf(null, null, null),
                    bookCount = 4,
                    finishedCount = 1,
                    progress = listOf(1F, 0.6F, 0F, 0F),
                    category = BookCategory.CURRENT,
                    bookIds = List(4) { BookId(Uuid.random().toString()) },
                  ),
                ),
              )
              repeat(9) {
                val book = book()
                add(LibraryItem.Book(book.id, mutableStateOf(book)))
              }
            },
          BookCategory.FINISHED to
            List(2) {
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
      showFilterIcon = true,
      isLoading = true,
      showStoragePermissionBugCard = false,
      showSurveyCard = false,
      showFolderAccessBadge = false,
      showFolderPickerIcon = true,
      dialog = null,
    )

  private val authors =
    library.copy(
      grouping = LibraryGrouping.Author,
      hero = null,
      sections =
        mapOf(
          BookCategory.CURRENT to library.sections.getValue(BookCategory.CURRENT).take(2),
          BookCategory.NOT_STARTED to
            listOf(
                author("Lena Morgan", 1, 4),
                author("Nora Whitfield", 0, 1),
                author(null, 0, 3),
              )
              .map { LibraryItem.Author(it.copy(category = BookCategory.NOT_STARTED)) },
          BookCategory.FINISHED to listOf(LibraryItem.Author(author("Daniel Hartwell", 0, 2))),
        ),
    )

  override val values =
    sequenceOf(
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
      authors,
      library.copy(selection = selecting(library)),
      library.copy(layoutMode = BookOverviewLayoutMode.Grid, selection = selecting(library)),
      authors.copy(layoutMode = BookOverviewLayoutMode.Grid, selection = selecting(authors)),
    )

  private fun selecting(state: BookOverviewViewState): SelectionViewState {
    val items = state.sections.values.flatten()
    val ids =
      listOfNotNull(state.hero?.value?.id) +
        items.filterIsInstance<LibraryItem.Book>().take(1).map { it.id } +
        items.filterIsInstance<LibraryItem.Author>().take(1).flatMap { it.author.bookIds }
    return SelectionViewState(ids = ids.toSet(), allSelected = false, allHidden = false)
  }

  private fun author(
    name: String?,
    seriesCount: Int,
    bookCount: Int,
  ) =
    AuthorEntryViewState(
      key = name?.lowercase(),
      name = name,
      covers = List(minOf(bookCount, 4)) { null },
      seriesCount = seriesCount,
      bookCount = bookCount,
      category = BookCategory.FINISHED,
      bookIds = List(bookCount) { BookId(Uuid.random().toString()) },
    )
}
