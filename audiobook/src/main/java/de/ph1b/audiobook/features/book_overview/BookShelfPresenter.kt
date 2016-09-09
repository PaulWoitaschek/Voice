package de.ph1b.audiobook.features.book_overview

import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayerController
import i
import rx.subscriptions.CompositeSubscription
import javax.inject.Inject

/**
 * Presenter for [BookShelfController].
 *
 * @author Paul Woitaschek
 */
class BookShelfPresenter
@Inject
constructor(private val bookChest: BookChest,
            private val bookAdder: BookAdder,
            private val prefsManager: PrefsManager,
            private val playStateManager: PlayStateManager,
            private val playerController: PlayerController)
: Presenter<BookShelfController>() {

    override fun onBind(view: BookShelfController, subscriptions: CompositeSubscription) {
        i { "onBind Called for $view" }

        val audioFoldersEmpty = prefsManager.collectionFolders.value().isEmpty() && prefsManager.singleBookFolders.value().isEmpty()
        if (audioFoldersEmpty) view.showNoFolderWarning()

        // scan for files
        bookAdder.scanForFiles(false)

        subscriptions.apply {

            // update books when they changed
            add(bookChest.booksStream().subscribe {
                view.newBooks(it)
                view.showSpinnerIfNoData(bookAdder.scannerActive.toBlocking().first())
            })

            // Subscription that notifies the adapter when the current book has changed. It also notifies
            // the item with the old indicator now falsely showing.
            add(prefsManager.currentBookId.asObservable()
                    .map { id -> bookChest.bookById(id) }
                    .subscribe { view.currentBookChanged(it) })

            // observe if the scanner is active and there are books and show spinner accordingly.
            add(bookAdder.scannerActive.subscribe { view.showSpinnerIfNoData(it) })

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