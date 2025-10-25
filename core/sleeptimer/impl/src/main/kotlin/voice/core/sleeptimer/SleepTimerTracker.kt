package voice.core.sleeptimer

import dev.zacsweers.metro.Inject
import voice.core.analytics.api.Analytics

@Inject
internal class SleepTimerTracker(private val analytics: Analytics) {

  fun enabled(mode: SleepTimerMode) {
    analytics.event(
      "sleep_timer_enabled",
      buildMap {
        put(
          "mode",
          when (mode) {
            SleepTimerMode.EndOfChapter -> "end_of_chapter"
            SleepTimerMode.TimedWithDefault -> "timed_with_default"
            is SleepTimerMode.TimedWithDuration -> "timed_with_duration"
          },
        )
        if (mode is SleepTimerMode.TimedWithDuration) {
          put("duration_minutes", mode.duration.inWholeMinutes.toString())
        }
      },
    )
  }

  fun disabled() {
    analytics.event("sleep_timer_disabled")
  }
}
