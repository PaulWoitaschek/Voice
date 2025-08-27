package voice.core.sleeptimer

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface SleepTimer {
  val leftSleepTimeFlow: Flow<Duration>
  val sleepAtEocFlow: Flow<Boolean>
  val sleepAtEoc: Boolean
  fun sleepTimerActive(): Boolean
  fun enable(mode: SleepTimerMode)

  fun disable()
}

sealed interface SleepTimerMode {
  data class TimedWithDuration(val duration: Duration) : SleepTimerMode
  data object TimedWithDefault : SleepTimerMode
  object EndOfChapter : SleepTimerMode
}
