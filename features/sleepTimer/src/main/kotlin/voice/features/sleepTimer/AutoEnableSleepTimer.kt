package voice.features.sleepTimer

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.common.pref.CurrentBookStore
import voice.core.common.pref.SleepTimerPreferenceStore
import voice.core.common.sleepTimer.SleepTimerPreference
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.playback.playstate.PlayStateManager
import voice.core.playback.playstate.PlayStateManager.PlayState.Playing
import voice.core.playback.session.SleepTimer
import java.time.Clock

@Inject
class AutoEnableSleepTimer(
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
