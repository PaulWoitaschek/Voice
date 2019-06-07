package de.ph1b.audiobook.features.bookPlaying

import de.ph1b.audiobook.common.Optional
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.data.repo.BookmarkRepo
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.PlayerCommand
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.SleepTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

class BookPlayPresenter(private val bookId: UUID) : BookPlayMvp.Presenter() {

  @Inject
  lateinit var bookRepository: BookRepository
  @Inject
  lateinit var playerController: PlayerController
  @Inject
  lateinit var playStateManager: PlayStateManager
  @Inject
  lateinit var sleepTimer: SleepTimer
  @Inject
  lateinit var bookmarkRepo: BookmarkRepo
  @field:[Inject Named(PrefKeys.CURRENT_BOOK)]
  lateinit var currentBookIdPref: Pref<UUID>

  init {
    appComponent.inject(this)
  }

  override fun onAttach(view: BookPlayMvp.View) {
    currentBookIdPref.value = bookId
    playStateManager.playStateStream()
      .map { it == PlayState.PLAYING }
      .distinctUntilChanged()
      .subscribe {
        Timber.i("onNext with playing=$it")
        view.showPlaying(it)
      }
      .disposeOnDetach()

    sleepTimer.leftSleepTimeInMsStream
      .subscribe { view.showLeftSleepTime(it.toInt()) }
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
    playerController.execute(PlayerCommand.PlayPause)
  }

  override fun rewind() {
    playerController.execute(PlayerCommand.Rewind)
  }

  override fun fastForward() {
    playerController.execute(PlayerCommand.FastForward)
  }

  override fun next() {
    playerController.execute(PlayerCommand.Next)
  }

  override fun previous() {
    playerController.execute(PlayerCommand.Previous)
  }

  override fun seekTo(position: Int, file: File?) {
    Timber.i("seekTo position$position, file$file")
    val book = bookRepository.bookById(bookId)
      ?: return
    playerController.execute(PlayerCommand.SetPosition(position, file ?: book.content.currentFile))
  }

  override fun toggleSkipSilence() {
    val skipSilence = bookRepository.bookById(bookId)?.content?.skipSilence
      ?: return
    playerController.execute(PlayerCommand.SkipSilence(!skipSilence))
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
