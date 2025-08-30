package voice.core.sleeptimer

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.sleeptimer.SleepTimerPreference
import voice.core.data.store.SleepTimerPreferenceStore
import voice.core.playback.playstate.PlayStateManager
import voice.core.playback.playstate.PlayStateManager.PlayState.Playing
import java.time.Clock

@Inject
class AutoEnableSleepTimer(
  @SleepTimerPreferenceStore
  private val sleepTimerPreferenceStore: DataStore<SleepTimerPreference>,
  dispatcherProvider: DispatcherProvider,
  private val playStateManager: PlayStateManager,
  private val sleepTimer: SleepTimer,
  private val clock: Clock,
  private val createBookmarkAtCurrentPosition: CreateBookmarkAtCurrentPosition,
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
              sleepTimerActive = sleepTimer.state.value.enabled,
            )
          ) {
            sleepTimer.enable(SleepTimerMode.TimedWithDefault)
            createBookmark()
          }
        }
    }
  }

  private suspend fun createBookmark() {
    createBookmarkAtCurrentPosition.create()
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
