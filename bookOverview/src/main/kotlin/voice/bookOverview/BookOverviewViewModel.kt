package voice.bookOverview

import android.text.format.DateUtils
import androidx.datastore.core.DataStore
import de.paulwoitaschek.flowpref.Pref
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import voice.app.scanner.MediaScanTrigger
import voice.common.pref.CurrentBook
import voice.common.pref.PrefKeys
import voice.data.Book
import voice.data.repo.BookRepository
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
  private val currentBookDataStore: DataStore<Book.Id?>,
  @Named(PrefKeys.GRID_MODE)
  private val gridModePref: Pref<GridMode>,
  private val gridCount: GridCount,
) {

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
      gridModePref.flow
    ) { playState, books, currentBookId, scannerActive, gridMode ->
      val noBooks = !scannerActive && books.isEmpty()
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
                  cover = book.content.cover,
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
        showAddBookHint = noBooks
      )
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
