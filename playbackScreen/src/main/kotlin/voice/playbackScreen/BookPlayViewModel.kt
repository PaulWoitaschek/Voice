package voice.playbackScreen

import androidx.datastore.core.DataStore
import de.ph1b.audiobook.common.pref.CurrentBook
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.durationMs
import de.ph1b.audiobook.data.markForPosition
import de.ph1b.audiobook.data.repo.BookRepo2
import de.ph1b.audiobook.data.repo.BookmarkRepo
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.playstate.PlayStateManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.sleepTimer.SleepTimer
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class BookPlayViewModel
@Inject constructor(
  private val repo: BookRepo2,
  private val player: PlayerController,
  private val sleepTimer: SleepTimer,
  private val playStateManager: PlayStateManager,
  private val bookmarkRepo: BookmarkRepo,
  @CurrentBook
  private val currentBookId: DataStore<Book2.Id?>,
) {

  private val scope = MainScope()

  private val _viewEffects = MutableSharedFlow<BookPlayViewEffect>(extraBufferCapacity = 1)
  val viewEffects: Flow<BookPlayViewEffect> get() = _viewEffects

  lateinit var bookId: Book2.Id

  fun viewState(): Flow<BookPlayViewState> {
    scope.launch {
      currentBookId.updateData { bookId }
    }

    return combine(
      repo.flow(bookId).filterNotNull(), playStateManager.playStateFlow(), sleepTimer.leftSleepTimeFlow
    ) { book, playState, sleepTime ->
      val currentMark = book.currentChapter.markForPosition(book.content.positionInChapter)
      val hasMoreThanOneChapter = book.chapters.sumOf { it.chapterMarks.count() } > 1
      BookPlayViewState(
        sleepTime = sleepTime,
        playing = playState == PlayStateManager.PlayState.Playing,
        title = book.content.name,
        showPreviousNextButtons = hasMoreThanOneChapter,
        chapterName = currentMark.name.takeIf { hasMoreThanOneChapter },
        duration = currentMark.durationMs.milliseconds,
        playedTime = (book.content.positionInChapter - currentMark.startMs).milliseconds,
        cover = book.content.cover,
        skipSilence = book.content.skipSilence
      )
    }
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
    scope.launch {
/*
todo
      val book = repo.bookById(bookId) ?: return@launch
      bookmarkRepo.addBookmarkAtBookPosition(
        book = book,
        title = null,
        setBySleepTimer = false
      )
      _viewEffects.emit(BookPlayViewEffect.BookmarkAdded)
*/
    }
  }

  fun seekTo(ms: Long) {
    scope.launch {
      val book = repo.flow(bookId).first() ?: return@launch
      val currentChapter = book.currentChapter
      val currentMark = currentChapter.markForPosition(book.content.positionInChapter)
      player.setPosition(currentMark.startMs + ms, currentChapter.uri)
    }
  }

  fun toggleSleepTimer() {
    if (sleepTimer.sleepTimerActive()) {
      sleepTimer.setActive(false)
    } else {
      _viewEffects.tryEmit(BookPlayViewEffect.ShowSleepTimeDialog)
    }
  }

  fun toggleSkipSilence() {
    scope.launch {
      val book = repo.flow(bookId).first() ?: return@launch
      val skipSilence = book.content.skipSilence
      player.skipSilence(!skipSilence)
    }
  }
}
