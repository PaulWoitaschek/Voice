package voice.core.audiobookshelf

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.FeatureFlagFactory

@Qualifier
annotation class AudiobookshelfFeatureFlagQualifier

@BindingContainer
@ContributesTo(AppScope::class)
object AudiobookshelfFeatureFlags {

  @Provides
  @SingleIn(AppScope::class)
  @AudiobookshelfFeatureFlagQualifier
  fun audiobookshelfFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> {
    return factory.boolean(
      key = "audiobookshelf",
      description = "Lets listeners connect an Audiobookshelf server. A server that is connected stays, also when this is off.",
    )
  }

  @Provides
  @IntoSet
  fun audiobookshelfFeatureFlagIntoSet(@AudiobookshelfFeatureFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<*> = flag
}
