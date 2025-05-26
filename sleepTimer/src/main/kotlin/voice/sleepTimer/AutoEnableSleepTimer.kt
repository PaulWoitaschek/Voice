package voice.sleepTimer

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.filter
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
import java.time.Clock
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
  private val clock: Clock,
) {

  private val mainScope = MainScope(dispatcherProvider)

  fun startMonitoring() {
    mainScope.launch {
      playStateManager.flow
        .filter { it == Playing }
        .collect {
          val autoSleepTimerPreference = sleepTimerPreferenceStore.data.first()
          if (shouldEnableSleepTimer(
              autoSleepTimer = autoSleepTimerPreference,
              sleepTimerActive = sleepTimer.sleepTimerActive(),
            )
          ) {
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
    autoSleepTimer: SleepTimerPreference,
    sleepTimerActive: Boolean,
  ): Boolean {
    return autoSleepTimer.autoSleepTimerEnabled &&
      !sleepTimerActive &&
      isTimeInRange(
        currentTime = clock.instant().atZone(clock.zone).toLocalTime(),
        startTime = autoSleepTimer.autoSleepStartTime,
        endTime = autoSleepTimer.autoSleepEndTime,
      )
  }
}
