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
import i
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

/**
 * Presenter for [BookShelfController].
 *
 * @author Paul Woitaschek
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

  override fun onBind(view: BookShelfController, disposables: CompositeDisposable) {
    i { "onBind Called for $view" }

    val audioFoldersEmpty = prefsManager.collectionFolders.value.isEmpty() && prefsManager.singleBookFolders.value.isEmpty()
    if (audioFoldersEmpty) view.showNoFolderWarning()

    // scan for files
    bookAdder.scanForFiles()

    disposables.apply {
      // update books when they changed
      add(repo.booksStream().subscribe {
        view.displayNewBooks(it)
      })

      // Subscription that notifies the adapter when the current book has changed. It also notifies
      // the item with the old indicator now falsely showing.
      add(prefsManager.currentBookId.asV2Observable()
          .subscribe {
            val book = repo.bookById(it)
            view.updateCurrentBook(book)
          })

      // if there are no books and the scanner is active, show loading
      add(combineLatest(bookAdder.scannerActive, repo.booksStream().map { it.isEmpty() }) { active, booksEmpty ->
        if (booksEmpty) active else false
      }.subscribe { view.showLoading(it) })

      // Subscription that updates the UI based on the play state.
      add(playStateManager.playStateStream().subscribe {
        val playing = it == PlayState.PLAYING
        view.showPlaying(playing)
      })

      // notify view when a book cover changed
      add(coverFromDiscCollector.coverChanged()
          .subscribe { view.bookCoverChanged(it) })
    }
  }

  fun playPauseRequested() = playerController.playPause()
}