package voice.core.sleeptimer

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Decides whether the device is being shaken based on a sliding window of accelerometer samples.
 *
 * A shake is reported once the samples span at least [MIN_WINDOW] and about three quarters of them are accelerating.
 * Samples older than [MAX_WINDOW] are dropped, while keeping at least [MIN_SAMPLES] samples.
 */
internal class ShakeWindow {

  private val samples = ArrayDeque<Sample>()
  private var acceleratingCount = 0

  /**
   * Adds a sample and returns whether the device is being shaken.
   */
  fun add(
    timestamp: Duration,
    accelerating: Boolean,
  ): Boolean {
    removeSamplesBefore(timestamp - MAX_WINDOW)
    samples.addLast(Sample(timestamp, accelerating))
    if (accelerating) acceleratingCount++
    return isShaking()
  }

  private fun removeSamplesBefore(cutoff: Duration) {
    while (samples.size >= MIN_SAMPLES && samples.first().timestamp < cutoff) {
      if (samples.removeFirst().accelerating) acceleratingCount--
    }
  }

  private fun isShaking(): Boolean {
    val span = samples.last().timestamp - samples.first().timestamp
    return span >= MIN_WINDOW && acceleratingCount >= samples.size / 2 + samples.size / 4
  }

  private class Sample(
    val timestamp: Duration,
    val accelerating: Boolean,
  )

  private companion object {
    val MAX_WINDOW = 500.milliseconds
    val MIN_WINDOW = MAX_WINDOW / 2
    const val MIN_SAMPLES = 4
  }
}
