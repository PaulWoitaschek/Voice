package voice.sleepTimer

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.common.BookId
import voice.common.DispatcherProvider
import voice.common.MainScope
import voice.common.pref.CurrentBookStore
import voice.common.pref.SleepTimerPreferenceStore
import voice.common.sleepTimer.SleepTimerPreference
import voice.data.repo.BookRepository
import voice.data.repo.BookmarkRepo
import voice.playback.playstate.PlayStateManager
import voice.playback.playstate.PlayStateManager.PlayState.Playing
import voice.playback.session.SleepTimer
import java.time.LocalTime
import javax.inject.Inject

class AutoEnableSleepTimer
@Inject constructor(
  @SleepTimerPreferenceStore
  private val sleepTimerPreferenceStore: DataStore<SleepTimerPreference>,
  dispatcherProvider: DispatcherProvider,
  private val playStateManager: PlayStateManager,
  private val sleepTimer: SleepTimer,
  private val bookmarkRepo: BookmarkRepo,
  private val bookRepository: BookRepository,
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
) {

  private val mainScope = MainScope(dispatcherProvider)

  fun startMonitoring() {
    mainScope.launch {
      combine(
        playStateManager.flow,
        sleepTimerPreferenceStore.data,
      ) { playState, autoSleepTimer ->
        playState to autoSleepTimer
      }.collect { (playState, autoSleepTimer) ->
        if (shouldEnableSleepTimer(playState, autoSleepTimer)) {
          sleepTimer.setActive(true)
          createBookmark()
        }
      }
    }
  }

  private suspend fun createBookmark() {
    val currentBookId = currentBookStore.data.first() ?: return
    val currentBook = bookRepository.get(currentBookId) ?: return
    bookmarkRepo.addBookmarkAtBookPosition(
      book = currentBook,
      title = null,
      setBySleepTimer = true,
    )
  }

  private fun shouldEnableSleepTimer(
    playState: PlayStateManager.PlayState,
    autoSleepTimer: SleepTimerPreference,
  ): Boolean {
    return playState == Playing &&
      autoSleepTimer.autoSleepTimerEnabled &&
      isTimeInRange(
        currentTime = LocalTime.now(),
        startTime = autoSleepTimer.autoSleepStartTime,
        endTime = autoSleepTimer.autoSleepEndTime,
      )
  }
}
