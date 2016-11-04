package de.ph1b.audiobook.features.book_overview

import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.misc.asV2Observable
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayerController
import i
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import javax.inject.Inject

/**
 * Presenter for [BookShelfController].
 *
 * @author Paul Woitaschek
 */
class BookShelfPresenter
@Inject
constructor(private val bookChest: BookRepository,
            private val bookAdder: BookAdder,
            private val prefsManager: PrefsManager,
            private val playStateManager: PlayStateManager,
            private val playerController: PlayerController)
: Presenter<BookShelfController>() {

    override fun onBind(view: BookShelfController, disposables: CompositeDisposable) {
        i { "onBind Called for $view" }

        val audioFoldersEmpty = prefsManager.collectionFolders.value().isEmpty() && prefsManager.singleBookFolders.value().isEmpty()
        if (audioFoldersEmpty) view.showNoFolderWarning()

        // scan for files
        bookAdder.scanForFiles(false)

        disposables.apply {

            // update books when they changed
            add(bookChest.booksStream().subscribe {
                view.newBooks(it)
            })

            // Subscription that notifies the adapter when the current book has changed. It also notifies
            // the item with the old indicator now falsely showing.
            add(prefsManager.currentBookId.asV2Observable()
                    .map { id -> bookChest.bookById(id) }
                    .subscribe { view.currentBookChanged(it) })

            // if there are no books and the scanner is active, show loading
            add(Observable.combineLatest(bookAdder.scannerActive, bookChest.booksStream().map { it.isEmpty() }, BiFunction<Boolean, Boolean, Boolean> { active, booksEmpty ->
                if (booksEmpty) active else false
            }).subscribe { view.showLoading(it) })

            // Subscription that updates the UI based on the play state.
            add(playStateManager.playState
                    .map { it == PlayStateManager.PlayState.PLAYING }
                    .subscribe { view.setPlayerPlaying(it) })
        }
    }

    fun playPauseRequested() {
        playerController.playPause()
    }
}