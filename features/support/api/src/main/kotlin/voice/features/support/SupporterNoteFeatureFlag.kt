package voice.features.support

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.FeatureFlagFactory

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<FeatureFlag<*>>())
class SupporterNoteFeatureFlag(flag: FeatureFlag<String>) : FeatureFlag<String> by flag {

  @Inject
  constructor(factory: FeatureFlagFactory) : this(
    factory.string(
      key = "supporter_note",
      description = "A note on the support sheet, e.g. what the support paid for this month. Hidden when empty.",
      defaultValue = "",
    ),
  )
}
