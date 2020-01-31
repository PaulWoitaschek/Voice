package de.ph1b.audiobook.features.bookPlaying

import de.ph1b.audiobook.common.Optional
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.data.repo.BookmarkRepo
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.SleepTimer
import de.ph1b.audiobook.playback.playstate.PlayStateManager
import de.ph1b.audiobook.playback.playstate.PlayStateManager.PlayState
import de.ph1b.audiobook.prefs.Pref
import de.ph1b.audiobook.prefs.PrefKeys
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asObservable
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
      .map { it == PlayState.Playing }
      .distinctUntilChanged()
      .subscribe {
        Timber.i("onNext with playing=$it")
        view.showPlaying(it)
      }
      .disposeOnDetach()

    sleepTimer.leftSleepTimeFlow.asObservable()
      .observeOn(AndroidSchedulers.mainThread())
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

  override fun seekTo(position: Long, file: File?) {
    Timber.i("seekTo position$position, file$file")
    val book = bookRepository.bookById(bookId)
      ?: return
    playerController.setPosition(position, file ?: book.content.currentFile)
  }

  override fun toggleSkipSilence() {
    val skipSilence = bookRepository.bookById(bookId)?.content?.skipSilence
      ?: return
    playerController.skipSilence(!skipSilence)
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
