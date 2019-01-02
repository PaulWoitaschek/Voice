package de.ph1b.audiobook.features.bookOverview

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewModel
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.Observables
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.uitools.CoverFromDiscCollector
import io.reactivex.Observable
import java.util.LinkedHashMap
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

class BookOverviewViewModel
@Inject
constructor(
  private val repo: BookRepository,
  private val bookAdder: BookAdder,
  private val playStateManager: PlayStateManager,
  private val playerController: PlayerController,
  coverFromDiscCollector: CoverFromDiscCollector,
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>
) {

  fun attach() {
    bookAdder.scanForFiles()
  }

  val coverChanged: Observable<UUID> = coverFromDiscCollector.coverChanged()

  val state: Observable<BookOverviewState>
    get() {
      val bookStream = repo.booksStream()
      val currentBookIdStream = currentBookIdPref.stream
      val playingStream = playStateManager.playStateStream()
        .map { it == PlayState.PLAYING }
        .distinctUntilChanged()
      val scannerActiveStream = bookAdder.scannerActive
      return Observables
        .combineLatest(
          bookStream,
          currentBookIdStream,
          playingStream,
          scannerActiveStream
        ) { books, currentBookId, playing, scannerActive ->
          state(
            books = books,
            scannerActive = scannerActive,
            currentBookId = currentBookId,
            playing = playing
          )
        }
    }

  private fun state(
    books: List<Book>,
    scannerActive: Boolean,
    currentBookId: UUID?,
    playing: Boolean
  ): BookOverviewState {
    if (books.isEmpty()) {
      return if (scannerActive) {
        BookOverviewState.Loading
      } else {
        BookOverviewState.NoFolderSet
      }
    }

    return content(books = books, currentBookId = currentBookId, playing = playing)
  }

  private fun content(books: List<Book>, currentBookId: UUID?, playing: Boolean): BookOverviewState.Content {
    val currentBookPresent = books.any { it.id == currentBookId }

    val categoriesWithContents = LinkedHashMap<BookOverviewCategory, BookOverviewCategoryContent>()
    BookOverviewCategory.values().forEach { category ->
      val content = content(books, category, currentBookId)
      if (content != null) {
        categoriesWithContents[category] = content
      }
    }

    return BookOverviewState.Content(
      playing = playing,
      currentBookPresent = currentBookPresent,
      categoriesWithContents = categoriesWithContents
    )
  }

  private fun content(
    books: List<Book>,
    category: BookOverviewCategory,
    currentBookId: UUID?
  ): BookOverviewCategoryContent? {
    val booksOfCategory = books.filter(category.filter)
      .sortedWith(category.comparator)
    if (booksOfCategory.isEmpty()) {
      return null
    }
    val takeAtMost = when (category) {
      BookOverviewCategory.CURRENT -> 4
      BookOverviewCategory.NOT_STARTED -> 4
      BookOverviewCategory.FINISHED -> 2
    }
    val models = booksOfCategory.take(takeAtMost).map {
      BookOverviewModel(it, it.id == currentBookId)
    }
    val hasMore = models.size != booksOfCategory.size
    return BookOverviewCategoryContent(models, hasMore)
  }

  fun playPause() {
    playerController.playPause()
  }
}
