package voice.sleepTimer

import java.time.LocalTime

fun isTimeInRange(
  currentTime: LocalTime,
  startTime: LocalTime,
  endTime: LocalTime,
): Boolean {
  return if (startTime <= endTime) {
    // Standard case, start and end on the same day
    currentTime.isAfter(startTime) && currentTime.isBefore(endTime)
  } else {
    // Range wraps around midnight
    currentTime.isAfter(startTime) || currentTime.isBefore(endTime)
  }
}
