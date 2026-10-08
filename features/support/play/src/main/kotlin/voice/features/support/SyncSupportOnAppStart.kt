package voice.features.support

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.SupportDevelopmentFeatureFlagQualifier
import voice.core.initializer.AppInitializer

/**
 * Looks at the purchases on every start. A purchase that was pending finishes while Voice isn't
 * open, and Google Play refunds it unless it's acknowledged within three days. This also keeps the
 * badge growing and notices a subscription that ended.
 */
@ContributesIntoSet(AppScope::class)
class SyncSupportOnAppStart(
  private val backend: SupportBackend,
  @SupportDevelopmentFeatureFlagQualifier
  private val supportDevelopmentFeatureFlag: FeatureFlag<Boolean>,
) : AppInitializer {

  override fun onAppStart(application: Application) {
    if (supportDevelopmentFeatureFlag.get()) {
      backend.refresh()
    }
  }
}
