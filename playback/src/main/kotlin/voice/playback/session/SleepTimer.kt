package voice.playback.session

import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

interface SleepTimer {
  val leftSleepTimeFlow: Flow<Duration>
  fun sleepTimerActive(): Boolean
  fun setActive(enable: Boolean)
}
