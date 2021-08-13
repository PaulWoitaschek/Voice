package voice.playbackScreen

import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.durationMs
import de.ph1b.audiobook.data.markForPosition
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.data.repo.BookmarkRepo
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.playstate.PlayStateManager
import java.util.UUID
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import voice.sleepTimer.SleepTimer
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration

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
      repo.flow(bookId).filterNotNull(), playStateManager.playStateFlow(), sleepTimer.leftSleepTimeFlow
    ) { book, playState, sleepTime ->
      val currentMark = book.content.currentChapter.markForPosition(book.content.positionInChapter)
      val hasMoreThanOneChapter = book.hasMoreThanOneChapter()
      BookPlayViewState(
        sleepTime = sleepTime,
        playing = playState == PlayStateManager.PlayState.Playing,
        title = book.name,
        showPreviousNextButtons = hasMoreThanOneChapter,
        chapterName = currentMark.name.takeIf { hasMoreThanOneChapter },
        duration = Duration.milliseconds(currentMark.durationMs),
        playedTime = Duration.milliseconds((book.content.positionInChapter - currentMark.startMs)),
        cover = BookPlayCover(book.name, book.id),
        skipSilence = book.content.skipSilence
      )
    }
  }

  private fun Book.hasMoreThanOneChapter(): Boolean {
    val chapterCount = content.chapters.sumOf { it.chapterMarks.size }
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
    scope.launch {
      val book = repo.bookById(bookId) ?: return@launch
      bookmarkRepo.addBookmarkAtBookPosition(
        book = book,
        title = null,
        setBySleepTimer = false
      )
      _viewEffects.send(BookPlayViewEffect.BookmarkAdded)
    }
  }

  fun seekTo(ms: Long) {
    scope.launch {
      val book = repo.bookById(bookId) ?: return@launch
      val currentChapter = book.content.currentChapter
      val currentMark = currentChapter.markForPosition(book.content.positionInChapter)
      player.setPosition(currentMark.startMs + ms, currentChapter.file)
    }
  }

  fun toggleSleepTimer() {
    if (sleepTimer.sleepTimerActive()) {
      sleepTimer.setActive(false)
    } else {
      _viewEffects.trySend(BookPlayViewEffect.ShowSleepTimeDialog).isSuccess
    }
  }

  fun toggleSkipSilence() {
    scope.launch {
      val skipSilence = repo.bookById(bookId)?.content?.skipSilence
        ?: return@launch
      player.skipSilence(!skipSilence)
    }
  }
}
