package de.ph1b.audiobook.features.bookOverview

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
          when {
            books.isEmpty() && !scannerActive -> BookOverviewState.NoFolderSet
            if (books.isEmpty()) scannerActive else false -> BookOverviewState.Loading
            else -> {
              val currentBook = books.find { it.id == currentBookId }
              BookOverviewState.Content(
                books = books,
                currentBook = currentBook,
                playing = playing
              )
            }
          }
        }
    }

  fun playPause() {
    playerController.playPause()
  }
}
