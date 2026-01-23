package voice.core.analytics.firebase

import android.app.Application
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.core.data.store.AnalyticsConsentStore
import voice.core.initializer.AppInitializer
import voice.core.logging.api.Logger
import com.google.firebase.analytics.FirebaseAnalytics as GmsFirebaseAnalytics

@ContributesIntoSet(AppScope::class)
class EnableAnalyticsOnConsent(
  @AnalyticsConsentStore
  private val analyticsConsentStore: DataStore<Boolean>,
  private val analytics: GmsFirebaseAnalytics,
) : AppInitializer {

  private val scope = MainScope()

  override fun onAppStart(application: Application) {
    scope.launch {
      analyticsConsentStore.data.collect {
        Logger.d("Enabling analytics collection: $it")
        analytics.setAnalyticsCollectionEnabled(it)
      }
    }
  }
}
