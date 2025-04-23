package voice.pref

import kotlinx.serialization.Serializable

@Serializable
data class AutoSleepTimerPrefs(
  var enabled: Boolean,
  var startTime: String,
  var endTime: String,
)
