package voice.core.sleeptimer

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import voice.core.initializer.AppInitializer
import voice.core.playback.playstate.PlayStateManager
import voice.core.playback.playstate.PlayStateManager.PlayState.Playing
import java.time.Clock

@ContributesIntoSet(AppScope::class)
class AutoEnableSleepTimer(
  private val sleepTimerPreferenceStore: SleepTimerPreferenceStore,
  private val playStateManager: PlayStateManager,
  private val sleepTimer: SleepTimer,
  private val clock: Clock,
  private val scope: CoroutineScope,
) : AppInitializer {

  override fun onAppStart(application: Application) {
    playStateManager.playStateFlow
      .filter { it == Playing }
      .onEach {
        val autoSleepTimerPreference = sleepTimerPreferenceStore.data.first()
        if (shouldEnableSleepTimer(
            autoSleepTimer = autoSleepTimerPreference,
            sleepTimerActive = sleepTimer.state.value.enabled,
          )
        ) {
          sleepTimer.enable(SleepTimerMode.TimedWithDefault)
        }
      }
      .launchIn(scope)
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
