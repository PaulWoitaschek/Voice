package voice.sleepTimer

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import voice.common.DispatcherProvider
import voice.common.MainScope
import voice.common.autoSleepTimer.AutoSleepTimer
import voice.common.pref.AutoSleepTimerStore
import voice.playback.playstate.PlayStateManager
import voice.playback.playstate.PlayStateManager.PlayState.Playing
import voice.playback.session.SleepTimer
import java.time.LocalTime
import javax.inject.Inject

class AutoEnableSleepTimer
@Inject constructor(
  @AutoSleepTimerStore
  private val autoSleepTimerStore: DataStore<AutoSleepTimer>,
  dispatcherProvider: DispatcherProvider,
  private val playStateManager: PlayStateManager,
  private val sleepTimer: SleepTimer,
) {

  private val mainScope = MainScope(dispatcherProvider)

  fun startMonitoring() {
    mainScope.launch {
      combine(
        playStateManager.flow,
        autoSleepTimerStore.data,
      ) { playState, autoSleepTimer ->
        playState to autoSleepTimer
      }.collect { (playState, autoSleepTimer) ->
        if (shouldEnableSleepTimer(playState, autoSleepTimer)) {
          sleepTimer.setActive(true)
        }
      }
    }
  }

  private fun shouldEnableSleepTimer(
    playState: PlayStateManager.PlayState,
    autoSleepTimer: AutoSleepTimer,
  ): Boolean {
    return playState == Playing &&
      autoSleepTimer.enabled &&
      isTimeInRange(
        currentTime = LocalTime.now(),
        startTime = autoSleepTimer.startTime,
        endTime = autoSleepTimer.endTime,
      )
  }
}
