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

  fun restarted(trigger: RestartTrigger) {
    analytics.event(
      "sleep_timer_restarted",
      mapOf(
        "trigger" to
          when (trigger) {
            RestartTrigger.ShakeWhileFading -> "shake_while_fading"
            RestartTrigger.ShakeAfterPause -> "shake_after_pause"
            RestartTrigger.Play -> "play"
          },
      ),
    )
  }
}

internal enum class RestartTrigger {
  ShakeWhileFading,
  ShakeAfterPause,
  Play,
}
