package de.ph1b.audiobook.features.bookOverview

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.features.BookAdder
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

  private fun content(
    books: List<Book>,
    currentBookId: UUID?,
    playing: Boolean
  ): BookOverviewState.Content {
    val currentBook = books.find { it.id == currentBookId }

    val current = ArrayList<Book>()
    val notStarted = ArrayList<Book>()
    val completed = ArrayList<Book>()

    books.forEach { book ->
      val position = book.content.position
      val duration = book.content.duration
      val target = when {
        position == 0 -> notStarted
        position >= duration -> completed
        else -> current
      }
      target += book
    }

    return BookOverviewState.Content(
      currentBook = currentBook,
      playing = playing,
      completedBooks = completed,
      currentBooks = current,
      notStartedBooks = notStarted
    )
  }

  fun playPause() {
    playerController.playPause()
  }
}
