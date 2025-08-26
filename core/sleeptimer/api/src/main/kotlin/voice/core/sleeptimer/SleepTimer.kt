package voice.core.sleeptimer

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

public interface SleepTimer {
  val leftSleepTimeFlow: Flow<Duration>
  fun sleepTimerActive(): Boolean
  fun setActive(enable: Boolean)
  fun setActive(sleepTime: Duration)
}
