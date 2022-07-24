package voice.bookOverview.overview

import android.text.format.DateUtils
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import de.paulwoitaschek.flowpref.Pref
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import voice.app.scanner.MediaScanTrigger
import voice.bookOverview.BookMigrationExplanationQualifier
import voice.bookOverview.BookMigrationExplanationShown
import voice.bookOverview.GridCount
import voice.bookOverview.GridMode
import voice.common.BookId
import voice.common.combine
import voice.common.compose.ImmutableFile
import voice.common.navigation.Destination
import voice.common.navigation.Navigator
import voice.common.pref.CurrentBook
import voice.common.pref.PrefKeys
import voice.data.Book
import voice.data.repo.BookRepository
import voice.data.repo.internals.dao.LegacyBookDao
import voice.logging.core.Logger
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
) : ViewModel() {

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

  fun state(): Flow<BookOverviewViewState.Content> {
    return combine(
      playStateManager.flow,
      repo.flow(),
      currentBookDataStore.data,
      mediaScanner.scannerActive,
      gridModePref.flow,
      bookMigrationExplanationShown.data,
      suspend { legacyBookDao.bookMetaDataCount() != 0 }.asFlow(),
    ) { playState, books, currentBookId, scannerActive, gridMode, bookMigrationExplanationShown, hasLegacyBooks ->
      val noBooks = !scannerActive && books.isEmpty()
      val showMigrateHint = hasLegacyBooks && !bookMigrationExplanationShown
      BookOverviewViewState.Content(
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
          GridMode.LIST -> BookOverviewViewState.Content.LayoutMode.List
          GridMode.GRID -> BookOverviewViewState.Content.LayoutMode.Grid
          GridMode.FOLLOW_DEVICE -> if (gridCount.useGridAsDefault()) {
            BookOverviewViewState.Content.LayoutMode.Grid
          } else {
            BookOverviewViewState.Content.LayoutMode.List
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
                BookOverviewViewState.Content.BookViewState(
                  name = book.content.name,
                  author = book.content.author,
                  cover = book.content.cover?.let(::ImmutableFile),
                  id = book.id,
                  progress = book.progress(),
                  remainingTime = DateUtils.formatElapsedTime((book.duration - book.position) / 1000)
                )
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
      )
    }
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

  fun onBoomMigrationHelperConfirmClick() {
    scope.launch {
      bookMigrationExplanationShown.updateData { true }
    }
  }

  fun playPause() {
    playerController.playPause()
  }
}

private fun Book.progress(): Float {
  val globalPosition = position
  val totalDuration = duration
  val progress = globalPosition.toFloat() / totalDuration.toFloat()
  if (progress < 0F) {
    Logger.w("Couldn't determine progress for book=$this")
  }
  return progress.coerceIn(0F, 1F)
}
