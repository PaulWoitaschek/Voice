package voice.bookOverview.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.datastore.core.DataStore
import de.paulwoitaschek.flowpref.Pref
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.app.scanner.MediaScanTrigger
import voice.bookOverview.BookMigrationExplanationQualifier
import voice.bookOverview.BookMigrationExplanationShown
import voice.bookOverview.GridCount
import voice.bookOverview.GridMode
import voice.common.BookId
import voice.common.navigation.Destination
import voice.common.navigation.Navigator
import voice.common.pref.CurrentBook
import voice.common.pref.PrefKeys
import voice.data.repo.BookRepository
import voice.data.repo.internals.dao.LegacyBookDao
import voice.playback.PlayerController
import voice.playback.playstate.PlayStateManager
import javax.inject.Inject
import javax.inject.Named

class BookOverviewViewModel
@Inject
constructor(
  private val repo: BookRepository,
  private val mediaScanner: MediaScanTrigger,
  private val playStateManager: PlayStateManager,
  private val playerController: PlayerController,
  @CurrentBook
  private val currentBookDataStore: DataStore<BookId?>,
  @Named(PrefKeys.GRID_MODE)
  private val gridModePref: Pref<GridMode>,
  private val gridCount: GridCount,
  @BookMigrationExplanationQualifier
  private val bookMigrationExplanationShown: BookMigrationExplanationShown,
  private val legacyBookDao: LegacyBookDao,
  private val navigator: Navigator,
) {

  private val scope = MainScope()

  fun attach() {
    mediaScanner.scan()
  }

  fun toggleGrid() {
    gridModePref.value = when (gridModePref.value) {
      GridMode.LIST -> GridMode.GRID
      GridMode.GRID -> GridMode.LIST
      GridMode.FOLLOW_DEVICE -> if (gridCount.useGridAsDefault()) {
        GridMode.LIST
      } else {
        GridMode.GRID
      }
    }
  }

  @Composable
  internal fun state(): BookOverviewViewState {
    val playState = playStateManager.flow.collectAsState(initial = PlayStateManager.PlayState.Stopped).value
    val books = repo.flow().collectAsState(initial = emptyList()).value
    val currentBookId = currentBookDataStore.data.collectAsState(initial = null).value
    val scannerActive = mediaScanner.scannerActive.collectAsState(initial = false).value
    val gridMode = gridModePref.flow.collectAsState(initial = null).value ?: return BookOverviewViewState.Loading
    val bookMigrationExplanationShown =
      bookMigrationExplanationShown.data.collectAsState(initial = null).value ?: return BookOverviewViewState.Loading

    val hasLegacyBooks = produceState<Boolean?>(initialValue = null) {
      value = legacyBookDao.bookMetaDataCount() != 0
    }.value ?: return BookOverviewViewState.Loading

    val noBooks = !scannerActive && books.isEmpty()
    val showMigrateHint = hasLegacyBooks && !bookMigrationExplanationShown

    return BookOverviewViewState.Content(
      layoutIcon = if (noBooks) {
        null
      } else {
        when (gridMode) {
          GridMode.LIST -> BookOverviewViewState.Content.LayoutIcon.Grid
          GridMode.GRID -> BookOverviewViewState.Content.LayoutIcon.List
          GridMode.FOLLOW_DEVICE -> if (gridCount.useGridAsDefault()) {
            BookOverviewViewState.Content.LayoutIcon.List
          } else {
            BookOverviewViewState.Content.LayoutIcon.Grid
          }
        }
      },
      layoutMode = when (gridMode) {
        GridMode.LIST -> BookOverviewLayoutMode.List
        GridMode.GRID -> BookOverviewLayoutMode.Grid
        GridMode.FOLLOW_DEVICE -> if (gridCount.useGridAsDefault()) {
          BookOverviewLayoutMode.Grid
        } else {
          BookOverviewLayoutMode.List
        }
      },
      books = books
        .groupBy {
          it.category
        }
        .mapValues { (category, books) ->
          books
            .sortedWith(category.comparator)
            .map { book ->
              book.toItemViewState()
            }
        }
        .toSortedMap(),
      playButtonState = if (playState == PlayStateManager.PlayState.Playing) {
        BookOverviewViewState.PlayButtonState.Playing
      } else {
        BookOverviewViewState.PlayButtonState.Paused
      }.takeIf { currentBookId != null },
      showAddBookHint = if (showMigrateHint) false else noBooks,
      showMigrateIcon = hasLegacyBooks,
      showMigrateHint = showMigrateHint,
      showSearchIcon = books.isNotEmpty(),
    )
  }

  fun onSettingsClick() {
    navigator.goTo(Destination.Settings)
  }

  fun onBookClick(id: BookId) {
    navigator.goTo(Destination.Playback(id))
  }

  fun onBookFolderClick() {
    navigator.goTo(Destination.FolderPicker)
  }

  fun onBookMigrationClick() {
    navigator.goTo(Destination.Migration)
  }

  fun onSearchClick() {
    navigator.goTo(Destination.BookSearch)
  }

  fun onBoomMigrationHelperConfirmClick() {
    scope.launch {
      bookMigrationExplanationShown.updateData { true }
    }
  }

  fun playPause() {
    playerController.playPause()
  }
}
