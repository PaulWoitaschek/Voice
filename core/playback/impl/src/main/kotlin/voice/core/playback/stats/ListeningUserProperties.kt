package voice.core.playback.stats

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import voice.core.analytics.api.Analytics
import voice.core.data.repo.ListeningStatsRepo
import voice.core.initializer.AppInitializer

@ContributesIntoSet(AppScope::class)
@Inject
class ListeningUserProperties(
  private val analytics: Analytics,
  private val repo: ListeningStatsRepo,
  private val clock: Clock,
  private val scope: CoroutineScope,
) : AppInitializer {

  override fun onAppStart(application: Application) {
    scope.launch {
      val summary = repo.summary(from = clock.now() - 30.days)
      analytics.setUserProperty("listening_time", timeRange(summary.listened))
      analytics.setUserProperty(
        "listening_speed",
        // analytics keeps the last value, so an old speed must not stay
        if (summary.listened > Duration.ZERO) speedRange(summary.audio / summary.listened)
        else "none",
      )
    }
  }
}

internal fun timeRange(listened: Duration): String =
  when {
    listened <= Duration.ZERO -> "none"
    listened < 1.hours -> "under_1_h"
    listened < 2.hours -> "1_to_2_h"
    listened < 5.hours -> "2_to_5_h"
    listened < 10.hours -> "5_to_10_h"
    listened < 20.hours -> "10_to_20_h"
    listened < 40.hours -> "20_to_40_h"
    listened < 70.hours -> "40_to_70_h"
    listened < 100.hours -> "70_to_100_h"
    listened < 150.hours -> "100_to_150_h"
    else -> "above_150_h"
  }

internal fun speedRange(speed: Double): String {
  // the times are rounded, so a listener at 1.25x can be a little above it
  val percent = (speed * 100).roundToInt()
  return when {
    percent < 95 -> "slower_than_1x"
    percent <= 105 -> "1x"
    percent <= 125 -> "up_to_1_25x"
    percent <= 150 -> "up_to_1_5x"
    percent <= 175 -> "up_to_1_75x"
    percent <= 200 -> "up_to_2x"
    percent <= 250 -> "up_to_2_5x"
    percent <= 300 -> "up_to_3x"
    else -> "faster_than_3x"
  }
}
