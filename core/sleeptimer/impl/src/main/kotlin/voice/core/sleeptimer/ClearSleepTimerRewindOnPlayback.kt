package voice.core.sleeptimer

import android.app.Application
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import voice.core.data.sleeptimer.SleepTimerRewind
import voice.core.data.store.SleepTimerRewindStore
import voice.core.initializer.AppInitializer
import voice.core.playback.playstate.PlayStateManager
import voice.core.playback.playstate.PlayStateManager.PlayState.Playing

/**
 * Once playback continues (headset button, notification, shake, ...) the user has picked up where they wanted,
 * so the rewind offered after a finished sleep timer is no longer relevant.
 */
@ContributesIntoSet(AppScope::class)
class ClearSleepTimerRewindOnPlayback(
  private val playStateManager: PlayStateManager,
  @SleepTimerRewindStore
  private val sleepTimerRewindStore: DataStore<SleepTimerRewind?>,
  private val scope: CoroutineScope,
) : AppInitializer {

  override fun onAppStart(application: Application) {
    playStateManager.playStateFlow
      .filter { it == Playing }
      .onEach { sleepTimerRewindStore.updateData { null } }
      .launchIn(scope)
  }
}
