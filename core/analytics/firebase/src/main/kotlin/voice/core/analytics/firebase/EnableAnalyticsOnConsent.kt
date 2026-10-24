package voice.core.analytics.firebase

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.core.analytics.api.AnalyticsConsentStore
import voice.core.initializer.AppInitializer
import com.google.firebase.analytics.FirebaseAnalytics as GmsFirebaseAnalytics

@ContributesIntoSet(AppScope::class)
class EnableAnalyticsOnConsent(
  private val analyticsConsentStore: AnalyticsConsentStore,
  private val analytics: GmsFirebaseAnalytics,
) : AppInitializer {

  private val scope = MainScope()

  override fun onAppStart(application: Application) {
    scope.launch {
      analyticsConsentStore.data.collect {
        analytics.setAnalyticsCollectionEnabled(it)
      }
    }
  }
}
