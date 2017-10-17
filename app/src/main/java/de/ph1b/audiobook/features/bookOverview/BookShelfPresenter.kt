package de.ph1b.audiobook.features.bookOverview

import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.Observables
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.uitools.CoverFromDiscCollector
import javax.inject.Inject
import javax.inject.Named

class BookShelfPresenter
@Inject
constructor(
    private val repo: BookRepository,
    private val bookAdder: BookAdder,
    private val playStateManager: PlayStateManager,
    private val playerController: PlayerController,
    private val coverFromDiscCollector: CoverFromDiscCollector,
    @Named(PrefKeys.CURRENT_BOOK)
    private val currentBookIdPref: Pref<Long>
) : Presenter<BookShelfView>() {

  override fun onAttach(view: BookShelfView) {
    bookAdder.scanForFiles()
    setupBookStream()
    setupCurrentBookStream()
    setupLoadingState()
    setupPlayState()
    setupCoverChanged()
    handleFolderWarning()
  }

  private fun setupCurrentBookStream() {
    currentBookIdPref.stream
        .subscribe {
          val book = repo.bookById(it)
          view.updateCurrentBook(book)
        }
        .disposeOnDetach()
  }

  private fun setupBookStream() {
    repo.booksStream()
        .subscribe { view.displayNewBooks(it) }
        .disposeOnDetach()
  }

  private fun setupLoadingState() {
    val noBooks = repo.booksStream().map { it.isEmpty() }
    val showLoading = Observables.combineLatest(bookAdder.scannerActive, noBooks) { active, booksEmpty ->
      if (booksEmpty) active else false
    }
    showLoading.subscribe { view.showLoading(it) }
        .disposeOnDetach()
  }

  private fun setupCoverChanged() {
    coverFromDiscCollector.coverChanged()
        .subscribe { view.bookCoverChanged(it) }
        .disposeOnDetach()
  }

  private fun setupPlayState() {
    playStateManager.playStateStream()
        .map { it == PlayState.PLAYING }
        .distinctUntilChanged()
        .subscribe { view.showPlaying(it) }
        .disposeOnDetach()
  }

  private fun handleFolderWarning() {
    val showFolderWarning = Observables.combineLatest(bookAdder.scannerActive, repo.booksStream()) { scannerActive, books ->
      books.isEmpty() && !scannerActive
    }.filter { it }
        .firstOrError()
    showFolderWarning
        .subscribe { _ -> view.showNoFolderWarning() }
        .disposeOnDetach()
  }

  fun playPauseRequested() = playerController.playPause()
}
