package de.ph1b.audiobook.features.bookPlaying

import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.addTo
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.Sandman
import i
import io.reactivex.disposables.CompositeDisposable
import java.io.File
import javax.inject.Inject

/**
 * Presenter for the book play screen
 *
 * @author Paul Woitaschek
 */
class BookPlayPresenter(private val bookId: Long) : BookPlayMvp.Presenter() {

  @Inject lateinit var bookRepository: BookRepository
  @Inject lateinit var playerController: PlayerController
  @Inject lateinit var playStateManager: PlayStateManager
  @Inject lateinit var sandman: Sandman

  init {
    App.component.inject(this)
  }

  override fun onBind(view: BookPlayMvp.View, disposables: CompositeDisposable) {
    // current book
    bookRepository.booksStream().subscribe {
      val book = it.firstOrNull { it.id == bookId }
      if (book == null) {
        view.finish()
      } else view.render(book)
    }.addTo(disposables)

    // play state
    playStateManager.playStateStream().subscribe {
      // animate only if this is not the first run
      i { "onNext with playState $it" }
      val playing = it == PlayState.PLAYING
      view.showPlaying(playing)
    }.addTo(disposables)

    // update sleep timer state
    sandman.sleepSand
        .subscribe { view.showLeftSleepTime(it) }
        .addTo(disposables)
  }

  override fun playPause() {
    playerController.playPause()
  }

  override fun rewind() {
    playerController.rewind()
  }

  override fun fastForward() {
    playerController.fastForward()
  }

  override fun next() {
    playerController.next()
  }

  override fun previous() {
    playerController.previous()
  }

  override fun seekTo(position: Long, file: File?) {
    i { "seekTo position$position, file$file" }
    val book = bookRepository.bookById(bookId)
        ?: return
    playerController.changePosition(position.toInt(), file ?: book.currentFile)
  }

  override fun toggleSleepTimer() {
    if (sandman.sleepTimerActive()) sandman.setActive(false)
    else {
      view!!.openSleepTimeDialog()
    }
  }
}
