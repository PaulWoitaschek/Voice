package voice.app.features.bookOverview

import androidx.datastore.core.DataStore
import de.paulwoitaschek.flowpref.Pref
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import voice.app.features.bookOverview.list.BookOverviewViewState
import voice.app.features.bookOverview.list.header.BookOverviewCategory
import voice.app.features.gridCount.GridCount
import voice.app.scanner.MediaScanTrigger
import voice.common.pref.CurrentBook
import voice.common.pref.PrefKeys
import voice.data.Book
import voice.data.repo.BookRepository
import voice.playback.PlayerController
import voice.playback.playstate.PlayStateManager
import voice.playback.playstate.PlayStateManager.PlayState
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
    books: List<Book>,
    scannerActive: Boolean,
    currentBookId: Book.Id?,
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
    books: List<Book>,
    currentBookId: Book.Id?,
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
    books: List<Book>,
    category: BookOverviewCategory,
    currentBookId: Book.Id?,
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
