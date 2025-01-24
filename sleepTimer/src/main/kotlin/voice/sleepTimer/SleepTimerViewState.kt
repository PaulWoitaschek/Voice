package voice.sleepTimer

data class SleepTimerViewState(
  val customSleepTime: Int,
  val autoSleepTimer: Boolean,
  val autoSleepTimeStart: String,
  val autoSleepTimeEnd: String,
)
