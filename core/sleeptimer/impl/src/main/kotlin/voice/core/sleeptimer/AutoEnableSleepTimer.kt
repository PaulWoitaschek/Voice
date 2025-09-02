package voice.core.sleeptimer

import android.app.Application
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import voice.core.data.sleeptimer.SleepTimerPreference
import voice.core.data.store.SleepTimerPreferenceStore
import voice.core.initializer.AppInitializer
import voice.core.playback.playstate.PlayStateManager
import voice.core.playback.playstate.PlayStateManager.PlayState.Playing
import java.time.Clock

@ContributesIntoSet(AppScope::class)
@Inject
class AutoEnableSleepTimer(
  @SleepTimerPreferenceStore
  private val sleepTimerPreferenceStore: DataStore<SleepTimerPreference>,
  private val playStateManager: PlayStateManager,
  private val sleepTimer: SleepTimer,
  private val clock: Clock,
  private val createBookmarkAtCurrentPosition: CreateBookmarkAtCurrentPosition,
  private val scope: CoroutineScope,
) : AppInitializer {

  override fun onAppStart(application: Application) {
    playStateManager.flow
      .filter { it == Playing }
      .onEach {
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
      .launchIn(scope)
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
