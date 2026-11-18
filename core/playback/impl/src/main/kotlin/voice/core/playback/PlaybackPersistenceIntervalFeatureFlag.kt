package voice.core.playback

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.FeatureFlagFactory

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<FeatureFlag<*>>())
class PlaybackPersistenceIntervalFeatureFlag(flag: FeatureFlag<String>) :
  FeatureFlag<String> by flag {

  @Inject
  constructor(
    factory: FeatureFlagFactory,
  ) : this(
    factory.string(
      key = "experimental_playback_persistence_interval_seconds",
      description =
        "Seconds between position saves while a book plays, with the experimental playback persistence.",
      defaultValue = DEFAULT_SECONDS.toString(),
    ),
  )

  fun interval(): Duration = (get().toLongOrNull()?.takeIf { it > 0 } ?: DEFAULT_SECONDS).seconds
}

private const val DEFAULT_SECONDS = 30L
