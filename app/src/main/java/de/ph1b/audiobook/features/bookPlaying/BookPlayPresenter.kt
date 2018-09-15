package de.ph1b.audiobook.features.bookPlaying

import de.ph1b.audiobook.common.Optional
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.data.repo.BookmarkRepo
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.SleepTimer
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.io.File
import java.util.UUID

class BookPlayPresenter(private val bookId: UUID) : BookPlayMvp.Presenter(), KoinComponent {

  private val bookRepository: BookRepository by inject()
  private val playerController: PlayerController by inject()
  private val playStateManager: PlayStateManager by inject()
  private val sleepTimer: SleepTimer by inject()
  private val bookmarkRepo: BookmarkRepo by inject()

  override fun onAttach(view: BookPlayMvp.View) {
    playStateManager.playStateStream()
      .map { it == PlayState.PLAYING }
      .distinctUntilChanged()
      .subscribe {
        Timber.i("onNext with playing=$it")
        view.showPlaying(it)
      }
      .disposeOnDetach()

    sleepTimer.leftSleepTimeInMs
      .subscribe { view.showLeftSleepTime(it) }
      .disposeOnDetach()

    bookRepository.byId(bookId)
      .distinctUntilChanged()
      .subscribe {
        when (it) {
          is Optional.Present -> view.render(it.value)
          is Optional.Absent -> view.finish()
        }
      }
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
    Timber.i("seekTo position$position, file$file")
    val book = bookRepository.bookById(bookId)
      ?: return
    playerController.changePosition(position, file ?: book.content.currentFile)
  }

  override fun toggleSkipSilence() {
    val skipSilence = bookRepository.bookById(bookId)?.content?.skipSilence
      ?: return
    playerController.setSkipSilence(!skipSilence)
  }

  override fun toggleSleepTimer() {
    if (sleepTimer.sleepTimerActive()) sleepTimer.setActive(false)
    else {
      view.openSleepTimeDialog()
    }
  }

  override fun addBookmark() {
    GlobalScope.launch(Dispatchers.Main) {
      val book = bookRepository.bookById(bookId) ?: return@launch
      val title = book.content.currentChapter.name
      bookmarkRepo.addBookmarkAtBookPosition(book, title)
      view.showBookmarkAdded()
    }
  }
}
