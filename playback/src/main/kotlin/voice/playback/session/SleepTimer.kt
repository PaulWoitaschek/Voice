package voice.playback.session

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface SleepTimer {
  val leftSleepTimeFlow: Flow<Duration>
  fun sleepTimerActive(): Boolean
  fun setActive(enable: Boolean)
}
