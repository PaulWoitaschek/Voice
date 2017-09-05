package de.ph1b.audiobook.features.bookOverview

import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.combineLatest
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.BookRepository
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
    @Named(PrefKeys.COLLECTION_BOOK_FOLDERS)
    private val collectionBookFolderPref: Pref<Set<String>>,
    @Named(PrefKeys.SINGLE_BOOK_FOLDERS)
    private val singleBookFolderPref: Pref<Set<String>>,
    @Named(PrefKeys.CURRENT_BOOK)
    private val currentBookIdPref: Pref<Long>
) : Presenter<BookShelfView>() {

  override fun onAttach(view: BookShelfView) {
    bookAdder.scanForFiles()
    handleFolderWarning()
    setupBookStream()
    setupCurrentBookStream()
    setupLoadingState()
    setupPlayState()
    setupCoverChanged()
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
    val showLoading = combineLatest(bookAdder.scannerActive, noBooks) { active, booksEmpty ->
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
    val audioFoldersEmpty = collectionBookFolderPref.value.isEmpty() && singleBookFolderPref.value.isEmpty()
    if (audioFoldersEmpty) view.showNoFolderWarning()
  }

  fun playPauseRequested() = playerController.playPause()
}
