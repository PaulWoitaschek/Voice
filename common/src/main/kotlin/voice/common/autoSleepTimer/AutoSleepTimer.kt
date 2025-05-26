package voice.common.autoSleepTimer

import kotlinx.serialization.Serializable
import voice.common.serialization.LocalTimeSerializer
import java.time.LocalTime

@Serializable
data class AutoSleepTimer(
  val enabled: Boolean,
  @Serializable(with = LocalTimeSerializer::class)
  val startTime: LocalTime,
  @Serializable(with = LocalTimeSerializer::class)
  val endTime: LocalTime,
) {

  companion object {
    val Default = AutoSleepTimer(
      enabled = false,
      startTime = LocalTime.of(22, 0),
      endTime = LocalTime.of(6, 0),
    )
  }
}
