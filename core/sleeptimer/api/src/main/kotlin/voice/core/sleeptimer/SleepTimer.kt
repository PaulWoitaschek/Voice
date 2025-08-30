package voice.core.sleeptimer

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

interface SleepTimer {
  val state: StateFlow<SleepTimerState>
  fun enable(mode: SleepTimerMode)
  fun disable()
}

sealed interface SleepTimerState {

  data object Disabled : SleepTimerState
  sealed interface Enabled : SleepTimerState {
    data object WithEndOfChapter : Enabled

    @JvmInline
    value class WithDuration(val leftDuration: Duration) : Enabled
  }

  val enabled: Boolean
    get() = when (this) {
      Disabled -> false
      is Enabled -> true
    }
}

sealed interface SleepTimerMode {
  data class TimedWithDuration(val duration: Duration) : SleepTimerMode
  data object TimedWithDefault : SleepTimerMode
  object EndOfChapter : SleepTimerMode
}
