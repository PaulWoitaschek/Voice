package voice.playbackScreen

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import voice.common.BookId
import voice.common.compose.ImmutableFile
import voice.common.navigation.Destination
import voice.common.navigation.Navigator
import voice.common.pref.CurrentBook
import voice.data.durationMs
import voice.data.markForPosition
import voice.data.repo.BookRepository
import voice.data.repo.BookmarkRepo
import voice.playback.PlayerController
import voice.playback.misc.Decibel
import voice.playback.misc.VolumeGain
import voice.playback.playstate.PlayStateManager
import voice.sleepTimer.SleepTimer
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class BookPlayViewModel
@Inject constructor(
  private val repo: BookRepository,
  private val player: PlayerController,
  private val sleepTimer: SleepTimer,
  private val playStateManager: PlayStateManager,
  @CurrentBook
  private val currentBookId: DataStore<BookId?>,
  private val navigator: Navigator,
  private val bookmarkRepo: BookmarkRepo,
  private val volumeGainFormatter: VolumeGainFormatter,
  private val batteryOptimization: BatteryOptimization,
) {

  private var askedToIgnoreBatteryOptimization = false

  private val scope = MainScope()

  private val _viewEffects = MutableSharedFlow<BookPlayViewEffect>(extraBufferCapacity = 1)
  internal val viewEffects: Flow<BookPlayViewEffect> get() = _viewEffects

  lateinit var bookId: BookId

  private val _dialogState = mutableStateOf<BookPlayDialogViewState?>(null)
  internal val dialogState: State<BookPlayDialogViewState?> get() = _dialogState

  fun viewState(): Flow<BookPlayViewState> {
    scope.launch {
      currentBookId.updateData { bookId }
    }

    return combine(
      repo.flow(bookId).filterNotNull(),
      playStateManager.flow,
      sleepTimer.leftSleepTimeFlow,
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
        cover = book.content.cover?.let(::ImmutableFile),
        skipSilence = book.content.skipSilence,
      )
    }
  }

  fun dismissDialog() {
    _dialogState.value = null
  }

  fun onPlaybackSpeedChanged(speed: Float) {
    _dialogState.value = BookPlayDialogViewState.SpeedDialog(speed)
    player.setSpeed(speed)
  }

  fun onVolumeGainChanged(gain: Decibel) {
    _dialogState.value = volumeGainDialogViewState(gain)
    player.setGain(gain)
  }

  fun next() {
    player.next()
  }

  fun previous() {
    player.previous()
  }

  fun playPause() {
    if (playStateManager.playState != PlayStateManager.PlayState.Playing && !askedToIgnoreBatteryOptimization) {
      if (!batteryOptimization.isIgnoringBatteryOptimizations()) {
        _viewEffects.tryEmit(BookPlayViewEffect.RequestIgnoreBatteryOptimization)
      }
      askedToIgnoreBatteryOptimization = true
    }
    player.playPause()
  }

  fun rewind() {
    player.rewind()
  }

  fun fastForward() {
    player.fastForward()
  }

  fun onCurrentChapterClicked() {
    scope.launch {
      val book = repo.get(bookId) ?: return@launch
      val chapterMarks = book.chapters.flatMap {
        it.chapterMarks
      }
      val selectedIndex = chapterMarks.indexOf(book.currentMark)
      _dialogState.value = BookPlayDialogViewState.SelectChapterDialog(
        chapters = chapterMarks,
        selectedIndex = selectedIndex.takeUnless { it == -1 },
      )
    }
  }

  fun onChapterClicked(index: Int) {
    scope.launch {
      val book = repo.get(bookId) ?: return@launch
      var currentIndex = -1
      book.chapters.forEach { chapter ->
        chapter.chapterMarks.forEach { mark ->
          currentIndex++
          if (currentIndex == index) {
            player.setPosition(mark.startMs, chapter.id)
            _dialogState.value = null
            return@launch
          }
        }
      }
    }
  }

  fun onPlaybackSpeedIconClicked() {
    scope.launch {
      val playbackSpeed = repo.get(bookId)?.content?.playbackSpeed
      if (playbackSpeed != null) {
        _dialogState.value = BookPlayDialogViewState.SpeedDialog(playbackSpeed)
      }
    }
  }

  fun onVolumeGainIconClicked() {
    scope.launch {
      val content = repo.get(bookId)?.content
      if (content != null) {
        _dialogState.value = volumeGainDialogViewState(Decibel(content.gain))
      }
    }
  }

  private fun volumeGainDialogViewState(gain: Decibel): BookPlayDialogViewState.VolumeGainDialog {
    return BookPlayDialogViewState.VolumeGainDialog(
      gain = gain,
      maxGain = VolumeGain.MAX_GAIN,
      valueFormatted = volumeGainFormatter.format(gain),
    )
  }

  fun onBookmarkClicked() {
    navigator.goTo(Destination.Bookmarks(bookId))
  }

  fun onBookmarkLongClicked() {
    scope.launch {
      val book = repo.get(bookId) ?: return@launch
      bookmarkRepo.addBookmarkAtBookPosition(
        book = book,
        title = null,
        setBySleepTimer = false,
      )
      _viewEffects.tryEmit(BookPlayViewEffect.BookmarkAdded)
    }
  }

  fun seekTo(position: Duration) {
    scope.launch {
      val book = repo.get(bookId) ?: return@launch
      val currentChapter = book.currentChapter
      val currentMark = currentChapter.markForPosition(book.content.positionInChapter)
      player.setPosition(currentMark.startMs + position.inWholeMilliseconds, currentChapter.id)
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
      val book = repo.get(bookId) ?: return@launch
      val skipSilence = book.content.skipSilence
      player.skipSilence(!skipSilence)
    }
  }
}
