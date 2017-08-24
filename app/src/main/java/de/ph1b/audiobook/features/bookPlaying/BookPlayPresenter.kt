package de.ph1b.audiobook.features.bookPlaying

import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.Optional
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.SleepTimer
import i
import java.io.File
import javax.inject.Inject

class BookPlayPresenter(private val bookId: Long) : BookPlayMvp.Presenter() {

  @Inject lateinit var bookRepository: BookRepository
  @Inject lateinit var playerController: PlayerController
  @Inject lateinit var playStateManager: PlayStateManager
  @Inject lateinit var sleepTimer: SleepTimer

  init {
    App.component.inject(this)
  }

  override fun onAttach(view: BookPlayMvp.View) {
    bookRepository.booksStream()
        .map {
          val currentBook = it.firstOrNull { it.id == bookId }
          Optional.of(currentBook)
        }
        .distinctUntilChanged()
        .subscribe {
          when (it) {
            is Optional.Present -> view.render(it.value)
            is Optional.Absent -> view.finish()
          }
        }
        .disposeOnDetach()

    playStateManager.playStateStream()
        .map { it == PlayState.PLAYING }
        .distinctUntilChanged()
        .subscribe {
          i { "onNext with playing=$it" }
          view.showPlaying(it)
        }
        .disposeOnDetach()

    sleepTimer.leftSleepTimeInMs
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
    if (sleepTimer.sleepTimerActive()) sleepTimer.setActive(false)
    else {
      view.openSleepTimeDialog()
    }
  }
}
