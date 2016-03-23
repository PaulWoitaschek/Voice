/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.features.book_overview

import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayerController
import i
import rx.Observable
import rx.subscriptions.CompositeSubscription
import javax.inject.Inject

/**
 * Presenter for [BookShelfFragment].
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
: BookShelfBasePresenter() {

    override fun onBind(view: BookShelfFragment, subscriptions: CompositeSubscription) {
        i { "onBind Called for $view" }

        // initially updates the adapter with a new set of items
        view.newBooks(bookChest.activeBooks)
        view.showSpinnerIfNoData(bookAdder.scannerActive().value)

        val audioFoldersEmpty = prefsManager.collectionFolders.isEmpty() && prefsManager.singleBookFolders.isEmpty()
        if (audioFoldersEmpty) view.showNoFolderWarning()

        // scan for files
        bookAdder.scanForFiles(false)

        subscriptions.apply {
            // informs the view once a book was removed
            add(bookChest.removedObservable()
                    .subscribe { view.bookRemoved(it) })

            // Subscription that notifies the adapter when there is a new or updated book.
            add(Observable.merge(bookChest.updateObservable(), bookChest.addedObservable())
                    .subscribe {
                        view.bookAddedOrUpdated(it)
                        view.showSpinnerIfNoData(bookAdder.scannerActive().value)
                    })

            // Subscription that notifies the adapter when the current book has changed. It also notifies
            // the item with the old indicator now falsely showing.
            add(prefsManager.currentBookId
                    .map { id -> bookChest.bookById(id) }
                    .subscribe { view.currentBookChanged(it) })

            // observe if the scanner is active and there are books and show spinner accordingly.
            add(bookAdder.scannerActive()
                    .subscribe { view.showSpinnerIfNoData(it) })

            // Subscription that updates the UI based on the play state.
            add(playStateManager.playState
                    .map { it == PlayStateManager.PlayState.PLAYING }
                    .subscribe { view.setPlayerPlaying(it) })
        }
    }

    override fun playPauseRequested() {
        playerController.playPause()
    }
}