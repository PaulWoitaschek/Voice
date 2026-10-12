package voice.core.playback.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerMode
import voice.core.sleeptimer.SleepTimerState

class FakeSleepTimer : SleepTimer {
  override val state: StateFlow<SleepTimerState>
    get() = stateFlow

  private val stateFlow = MutableStateFlow<SleepTimerState>(SleepTimerState.Disabled)

  override fun enable(mode: SleepTimerMode) {
    stateFlow.value = when (mode) {
      is SleepTimerMode.TimedWithDuration -> SleepTimerState.Enabled.WithDuration(mode.duration)
      SleepTimerMode.TimedWithDefault -> error("TimedWithDefault is not used in these tests")
      SleepTimerMode.EndOfChapter -> SleepTimerState.Enabled.WithEndOfChapter
    }
  }

  override fun disable() {
    stateFlow.value = SleepTimerState.Disabled
  }
}
