package voice.core.analytics.firebase

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.core.analytics.api.AnalyticsConsentStore
import voice.core.initializer.AppInitializer

@ContributesIntoSet(AppScope::class)
class EnableAnalyticsOnConsent(
  private val analyticsConsentStore: AnalyticsConsentStore,
  internal val analytics: FirebaseAnalytics,
) : AppInitializer {

  private val scope = MainScope()

  override fun onAppStart(application: Application) {
    scope.launch {
      analyticsConsentStore.data.collect {
        analytics.setCollectionEnabled(it)
      }
    }
  }
}
