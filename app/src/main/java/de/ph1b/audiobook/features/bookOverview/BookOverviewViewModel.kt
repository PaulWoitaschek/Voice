package de.ph1b.audiobook.features.bookOverview

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.features.bookOverview.list.BookComparator
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewModel
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.Observables
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.uitools.CoverFromDiscCollector
import io.reactivex.Observable
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

    val currentModels = books.asSequence()
      .filter {
        val position = it.content.position
        val duration = it.content.duration
        position in 1 until duration
      }
      .sortedWith(BookComparator.BY_LAST_PLAYED)
      .take(4)
      .map { BookOverviewModel(it, it.id == currentBookId) }
      .toList()

    val notStartedModels = books.asSequence()
      .filter { it.content.position == 0 }
      .sortedWith(BookComparator.BY_DATE_ADDED)
      .take(2)
      .map { BookOverviewModel(it, it.id == currentBookId) }
      .toList()

    val completedModels = books.asSequence()
      .filter { it.content.position >= it.content.duration }
      .sortedWith(BookComparator.BY_LAST_PLAYED)
      .take(2)
      .map { BookOverviewModel(it, it.id == currentBookId) }
      .toList()

    return BookOverviewState.Content(
      playing = playing,
      currentBookPresent = currentBookPresent,
      completedBooks = completedModels,
      currentBooks = currentModels,
      notStartedBooks = notStartedModels
    )
  }

  fun playPause() {
    playerController.playPause()
  }
}
