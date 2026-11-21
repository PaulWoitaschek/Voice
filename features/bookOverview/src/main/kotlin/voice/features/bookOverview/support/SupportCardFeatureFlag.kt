package voice.features.bookOverview.support

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import voice.core.featureflag.EnumFeatureFlag
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.FeatureFlagFactory

enum class SupportCardLook {
  None,
  Letter,
  Book,
}

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<FeatureFlag<*>>())
class SupportCardFeatureFlag(flag: FeatureFlag<SupportCardLook>) :
  EnumFeatureFlag<SupportCardLook>, FeatureFlag<SupportCardLook> by flag {

  override val entries: List<SupportCardLook> = SupportCardLook.entries

  @Inject
  constructor(
    factory: FeatureFlagFactory,
  ) : this(
    factory.enum(
      key = "support_card_after_book",
      description = "The look of the support card in the library, one time, after a finished book.",
      defaultValue = SupportCardLook.None,
      entries = SupportCardLook.entries,
    ),
  )
}
