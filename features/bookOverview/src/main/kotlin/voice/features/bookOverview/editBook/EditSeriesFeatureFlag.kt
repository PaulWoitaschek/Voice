package voice.features.bookOverview.editBook

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.FeatureFlagFactory

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<FeatureFlag<*>>())
class EditSeriesFeatureFlag(flag: FeatureFlag<Boolean>) : FeatureFlag<Boolean> by flag {

  @Inject
  constructor(factory: FeatureFlagFactory) : this(
    factory.boolean(
      key = "edit_series",
      description = "Lets listeners edit the series and part of a book.",
      defaultValue = false,
    ),
  )
}
