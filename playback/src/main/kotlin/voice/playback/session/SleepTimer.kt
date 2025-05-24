package voice.playback.session

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface SleepTimer {
  val leftSleepTimeFlow: Flow<Duration>
  var sleepAtEoc: Boolean
  fun sleepTimerActive(): Boolean
  fun setActive(enable: Boolean)
  fun setEocActive(enable: Boolean)
}
