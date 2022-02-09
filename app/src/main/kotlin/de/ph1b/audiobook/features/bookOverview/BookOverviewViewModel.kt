package de.ph1b.audiobook.features.bookOverview

import androidx.datastore.core.DataStore
import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.common.pref.CurrentBook
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.repo.BookRepo2
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewViewState
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import de.ph1b.audiobook.features.gridCount.GridCount
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.playstate.PlayStateManager
import de.ph1b.audiobook.playback.playstate.PlayStateManager.PlayState
import de.ph1b.audiobook.scanner.MediaScanTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

class BookOverviewViewModel
@Inject
constructor(
  private val repo: BookRepo2,
  private val mediaScanner: MediaScanTrigger,
  private val playStateManager: PlayStateManager,
  private val playerController: PlayerController,
  @CurrentBook
  private val currentBookDataStore: DataStore<Book2.Id?>,
  @Named(PrefKeys.GRID_MODE)
  private val gridModePref: Pref<GridMode>,
  private val gridCount: GridCount,
) {

  fun attach() {
    mediaScanner.scan()
  }

  fun useGrid(useGrid: Boolean) {
    gridModePref.value = if (useGrid) GridMode.GRID else GridMode.LIST
  }

  fun state(): Flow<BookOverviewState> {
    val playingStream = playStateManager.playStateFlow()
      .map { it == PlayState.Playing }
      .distinctUntilChanged()
    return combine(
      repo.flow(),
      currentBookDataStore.data,
      playingStream,
      mediaScanner.scannerActive,
      gridModePref.flow
    ) { books, currentBookId, playing, scannerActive, gridMode ->
      state(
        books = books,
        scannerActive = scannerActive,
        currentBookId = currentBookId,
        playing = playing,
        gridMode = gridMode
      )
    }
  }

  private fun state(
    books: List<Book2>,
    scannerActive: Boolean,
    currentBookId: Book2.Id?,
    playing: Boolean,
    gridMode: GridMode
  ): BookOverviewState {
    if (books.isEmpty()) {
      return if (scannerActive) {
        BookOverviewState.Loading
      } else {
        BookOverviewState.NoFolderSet
      }
    }

    return content(
      books = books,
      currentBookId = currentBookId,
      playing = playing,
      gridMode = gridMode
    )
  }

  private fun content(
    books: List<Book2>,
    currentBookId: Book2.Id?,
    playing: Boolean,
    gridMode: GridMode
  ): BookOverviewState.Content {
    val currentBookPresent = books.any { it.id == currentBookId }

    val amountOfColumns = gridCount.gridColumnCount(gridMode)

    val categoriesWithContents = LinkedHashMap<BookOverviewCategory, BookOverviewCategoryContent>()
    BookOverviewCategory.values().forEach { category ->
      val content = content(books, category, currentBookId, amountOfColumns)
      if (content != null) {
        categoriesWithContents[category] = content
      }
    }

    return BookOverviewState.Content(
      playing = playing,
      currentBookPresent = currentBookPresent,
      categoriesWithContents = categoriesWithContents,
      columnCount = amountOfColumns
    )
  }

  private fun content(
    books: List<Book2>,
    category: BookOverviewCategory,
    currentBookId: Book2.Id?,
    amountOfColumns: Int
  ): BookOverviewCategoryContent? {
    val booksOfCategory = books.filter(category.filter).sortedWith(category.comparator)
    if (booksOfCategory.isEmpty()) {
      return null
    }
    val models = booksOfCategory.map { book ->
      BookOverviewViewState(book, amountOfColumns, currentBookId)
    }
    return BookOverviewCategoryContent(models)
  }

  fun playPause() {
    playerController.playPause()
  }
}
