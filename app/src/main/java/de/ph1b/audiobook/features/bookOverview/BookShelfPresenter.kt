package de.ph1b.audiobook.features.bookOverview

import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.misc.asV2Observable
import de.ph1b.audiobook.misc.combineLatest
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.uitools.CoverFromDiscCollector
import timber.log.Timber
import javax.inject.Inject

/**
 * Presenter for [BookShelfController].
 */
class BookShelfPresenter
@Inject
constructor(
    private val repo: BookRepository,
    private val bookAdder: BookAdder,
    private val prefsManager: PrefsManager,
    private val playStateManager: PlayStateManager,
    private val playerController: PlayerController,
    private val coverFromDiscCollector: CoverFromDiscCollector)
  : Presenter<BookShelfController>() {

  override fun onAttach(view: BookShelfController) {
    Timber.i("onBind Called for $view")

    val audioFoldersEmpty = prefsManager.collectionFolders.value.isEmpty() && prefsManager.singleBookFolders.value.isEmpty()
    if (audioFoldersEmpty) view.showNoFolderWarning()

    bookAdder.scanForFiles()

    repo.booksStream()
        .subscribe {
          view.displayNewBooks(it)
        }
        .disposeOnDetach()

    prefsManager.currentBookId.asV2Observable()
        .subscribe {
          val book = repo.bookById(it)
          view.updateCurrentBook(book)
        }
        .disposeOnDetach()

    val noBooks = repo.booksStream().map { it.isEmpty() }
    val showLoading = combineLatest(bookAdder.scannerActive, noBooks) { active, booksEmpty ->
      if (booksEmpty) active else false
    }
    showLoading.subscribe { view.showLoading(it) }
        .disposeOnDetach()

    playStateManager.playStateStream()
        .subscribe {
          val playing = it == PlayState.PLAYING
          view.showPlaying(playing)
        }
        .disposeOnDetach()

    coverFromDiscCollector.coverChanged()
        .subscribe { view.bookCoverChanged(it) }
        .disposeOnDetach()
  }

  fun playPauseRequested() = playerController.playPause()
}
