package voice.playbackScreen.view.jumpToPosition

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal fun jumpToPositionTime(value: String): Duration {
  var time = Duration.ZERO
  fun add(
    component: String,
    durationUnit: DurationUnit,
  ) {
    val intValue = component.toIntOrNull() ?: return
    time += intValue.toDuration(durationUnit)
  }
  add(value.takeLast(2), DurationUnit.SECONDS)
  add(value.dropLast(2).takeLast(2), DurationUnit.MINUTES)
  add(value.dropLast(4), DurationUnit.HOURS)
  return time
}
