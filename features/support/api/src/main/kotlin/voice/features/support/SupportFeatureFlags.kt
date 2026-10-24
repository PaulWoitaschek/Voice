package voice.features.support

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
annotation class SupporterNoteFeatureFlagQualifier

@BindingContainer
@ContributesTo(AppScope::class)
object SupportFeatureFlags {

  @Provides
  @SingleIn(AppScope::class)
  @SupporterNoteFeatureFlagQualifier
  fun supporterNoteFeatureFlag(factory: FeatureFlagFactory): FeatureFlag<String> {
    return factory.string(
      key = "supporter_note",
      description = "A note on the support sheet, e.g. what the support paid for this month. Hidden when empty.",
      defaultValue = "",
    )
  }

  @Provides
  @IntoSet
  fun supporterNoteFeatureFlagIntoSet(@SupporterNoteFeatureFlagQualifier flag: FeatureFlag<String>): FeatureFlag<*> = flag
}
