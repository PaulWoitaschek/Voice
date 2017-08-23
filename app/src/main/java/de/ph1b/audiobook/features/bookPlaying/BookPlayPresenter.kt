package de.ph1b.audiobook.features.bookPlaying

import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.Sandman
import i
import java.io.File
import javax.inject.Inject

/**
 * Presenter for the book play screen
 */
class BookPlayPresenter(private val bookId: Long) : BookPlayMvp.Presenter() {

  @Inject lateinit var bookRepository: BookRepository
  @Inject lateinit var playerController: PlayerController
  @Inject lateinit var playStateManager: PlayStateManager
  @Inject lateinit var sandman: Sandman

  init {
    App.component.inject(this)
  }

  override fun onAttach(view: BookPlayMvp.View) {
    bookRepository.booksStream()
        .subscribe {
          val book = it.firstOrNull { it.id == bookId }
          if (book == null) {
            view.finish()
          } else view.render(book)
        }
        .disposeOnDetach()

    playStateManager.playStateStream()
        .subscribe {
          i { "onNext with playState $it" }
          val playing = it == PlayState.PLAYING
          view.showPlaying(playing)
        }
        .disposeOnDetach()

    sandman.sleepSand
        .subscribe { view.showLeftSleepTime(it) }
        .disposeOnDetach()
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

  override fun seekTo(position: Int, file: File?) {
    i { "seekTo position$position, file$file" }
    val book = bookRepository.bookById(bookId)
        ?: return
    playerController.changePosition(position, file ?: book.currentFile)
  }

  override fun toggleSleepTimer() {
    if (sandman.sleepTimerActive()) sandman.setActive(false)
    else {
      view.openSleepTimeDialog()
    }
  }
}
