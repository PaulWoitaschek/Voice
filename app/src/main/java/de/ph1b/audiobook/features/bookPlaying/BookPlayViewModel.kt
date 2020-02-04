package de.ph1b.audiobook.features.bookPlaying

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.currentMark
import de.ph1b.audiobook.data.durationMs
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.data.repo.BookmarkRepo
import de.ph1b.audiobook.data.repo.flowById
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.SleepTimer
import de.ph1b.audiobook.playback.playstate.PlayStateManager
import de.ph1b.audiobook.prefs.Pref
import de.ph1b.audiobook.prefs.PrefKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.milliseconds

class BookPlayViewModel
@Inject constructor(
  private val repo: BookRepository,
  private val player: PlayerController,
  private val sleepTimer: SleepTimer,
  private val playStateManager: PlayStateManager,
  private val bookmarkRepo: BookmarkRepo,
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>
) {

  private val scope = MainScope()

  private val _viewEffects = BroadcastChannel<BookPlayViewEffect>(1)
  val viewEffects: Flow<BookPlayViewEffect> get() = _viewEffects.asFlow()

  lateinit var bookId: UUID

  fun viewState(): Flow<BookPlayViewState> {
    currentBookIdPref.value = bookId

    return combine(
      repo.flowById(bookId).filterNotNull(), playStateManager.playStateFlow(), sleepTimer.leftSleepTimeFlow
    ) { book, playState, sleepTime ->
      val currentMark = book.content.currentChapter.currentMark(book.content.positionInChapter)
      val hasMoreThanOneChapter = book.hasMoreThanOneChapter()
      BookPlayViewState(
        sleepTime = sleepTime,
        playing = playState == PlayStateManager.PlayState.Playing,
        title = book.name,
        showPreviousNextButtons = hasMoreThanOneChapter,
        chapterName = currentMark.name.takeIf { hasMoreThanOneChapter },
        duration = currentMark.durationMs.milliseconds,
        playedTime = (book.content.positionInChapter - currentMark.startMs).milliseconds,
        cover = BookPlayCover(book)
      )
    }
  }

  private fun Book.hasMoreThanOneChapter(): Boolean {
    val chapterCount = content.chapters.sumBy { it.chapterMarks.size }
    return chapterCount > 1
  }

  fun next() {
    player.next()
  }

  fun previous() {
    player.previous()
  }

  fun playPause() {
    player.playPause()
  }

  fun rewind() {
    player.rewind()
  }

  fun fastForward() {
    player.fastForward()
  }

  fun addBookmark() {
    scope.launch(Dispatchers.Main) {
      val book = repo.bookById(bookId) ?: return@launch
      val title = book.content.currentChapter.name
      bookmarkRepo.addBookmarkAtBookPosition(book, title)
      _viewEffects.send(BookPlayViewEffect.BookmarkAdded)
    }
  }

  fun seekTo(ms: Long) {
    val book = repo.bookById(bookId) ?: return
    val currentChapter = book.content.currentChapter
    val markForPosition = currentChapter.currentMark(ms)
    player.setPosition(markForPosition.startMs + ms, currentChapter.file)
  }

  fun toggleSleepTimer() {
    if (sleepTimer.sleepTimerActive()) {
      sleepTimer.setActive(false)
    } else {
      _viewEffects.offer(BookPlayViewEffect.ShowSleepTimeDialog)
    }
  }

  fun toggleSkipSilence() {
    val skipSilence = repo.bookById(bookId)?.content?.skipSilence
      ?: return
    player.skipSilence(!skipSilence)
  }
}
