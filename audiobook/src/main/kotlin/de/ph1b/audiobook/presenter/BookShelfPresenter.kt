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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.presenter

import android.os.Bundle
import de.ph1b.audiobook.fragment.BookShelfFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.mediaplayer.MediaPlayerController
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.model.BookAdder
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager
import nucleus.presenter.RxPresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The presenter for [BookShelfFragment]
 */
class BookShelfPresenter : RxPresenter<BookShelfFragment>() {

    @Inject internal lateinit var bookChest: BookChest
    @Inject internal lateinit var playStateManager: PlayStateManager
    @Inject internal lateinit var bookAdder: BookAdder
    @Inject internal lateinit var prefsManager: PrefsManager
    @Inject internal lateinit var mediaPlayerController: MediaPlayerController

    init {
        App.component().inject(this)
    }

    fun playPauseRequested() {
        mediaPlayerController.playPause()
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // informs the view once a book was removed
        add(bookChest.removedObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .buffer(100, TimeUnit.MILLISECONDS)
                .filter { it.isNotEmpty() }
                .subscribe {
                    view?.booksRemoved(it)
                })

        // Subscription that notifies the adapter when there is a new or updated book.
        add(Observable.merge(bookChest.updateObservable(), bookChest.addedObservable())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    view?.bookAddedOrUpdated(it)
                    view?.showSpinnerIfNoData(bookAdder.scannerActive().value)
                })


        // Subscription that notifies the adapter when the current book has changed. It also notifies
        // the item with the old indicator now falsely showing.
        add(prefsManager.currentBookId
                .flatMap { id ->
                    bookChest.activeBooks
                            .singleOrDefault(null, { it.id == id })
                }
                .compose(deliverLatestCache<Book?>())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(split { view, newBook ->
                    view.currentBookChanged(newBook)
                }))


        // observe if the scanner is active and there are books and show spinner accordingly.
        add(bookAdder.scannerActive() // scanner active
                .observeOn(AndroidSchedulers.mainThread())
                .compose(deliverLatestCache<Boolean>())
                .subscribe (split { view, scannerActive ->
                    Timber.i("scanner active observable set showSpinnerIfNoData to $scannerActive")
                    view.showSpinnerIfNoData(scannerActive)
                }))

        // Subscription that updates the UI based on the play state.
        add(playStateManager.playState
                .observeOn(AndroidSchedulers.mainThread())
                .compose(deliverLatestCache<PlayStateManager.PlayState>())
                .subscribe(split { view, playState ->
                    view.setPlayState(playState)
                }))
    }

    override fun onTakeView(view: BookShelfFragment) {
        super.onTakeView(view)

        // initially updates the adapter with a new set of items
        bookChest.activeBooks
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .toList()
                .subscribe {
                    view.newBooks(it)
                    view.showSpinnerIfNoData(bookAdder.scannerActive().value)
                }

        val audioFoldersEmpty = (prefsManager.collectionFolders.size + prefsManager.singleBookFolders.size) == 0
        if (audioFoldersEmpty) view.showNoFolderWarning()

        // scan for files
        bookAdder.scanForFiles(false)
    }
}