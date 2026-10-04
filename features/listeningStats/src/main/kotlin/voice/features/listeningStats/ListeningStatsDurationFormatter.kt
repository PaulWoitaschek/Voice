package voice.features.listeningStats

import kotlin.time.Duration.Companion.milliseconds

// The formatted result is split into hours and minutes so the UI does not recompute inside composables.
internal data class ListeningStatsDuration(
  val hours: Long,
  val minutes: Long,
) {
  // Zero hours switches to the shorter minutes-only format to reduce visual noise for short durations.
  val hasHours: Boolean get() = hours > 0
}

// The stats page shows minutes to avoid second-level fluctuations making the review look like debug data.
internal fun listeningStatsDuration(durationMs: Long): ListeningStatsDuration {
  val totalMinutes = durationMs.milliseconds.inWholeMinutes
  return ListeningStatsDuration(
    hours = totalMinutes / MINUTES_PER_HOUR,
    minutes = totalMinutes % MINUTES_PER_HOUR,
  )
}

// Fixed 60-minute hours keep the formatting formula easy to review.
private const val MINUTES_PER_HOUR = 60L
