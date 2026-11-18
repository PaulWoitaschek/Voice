package voice.core.analytics.firebase

import com.google.firebase.analytics.FirebaseAnalytics as GmsFirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import java.util.concurrent.ConcurrentHashMap
import voice.core.analytics.api.Analytics
import voice.core.logging.api.Logger

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class FirebaseAnalytics(private val analytics: GmsFirebaseAnalytics) : Analytics {

  // Firebase drops user properties while collection is off
  private val userProperties = ConcurrentHashMap<String, String>()

  override fun screenView(screenName: String) {
    Logger.v("screenView($screenName)")
    analytics.logEvent(GmsFirebaseAnalytics.Event.SCREEN_VIEW) {
      param(GmsFirebaseAnalytics.Param.SCREEN_NAME, screenName)
    }
  }

  override fun event(
    name: String,
    params: Map<String, String>,
  ) {
    Logger.v("event(name=$name, params=$params)")
    analytics.logEvent(name) {
      params.forEach { (key, value) ->
        param(key, value)
      }
    }
  }

  override fun setUserProperty(
    name: String,
    value: String,
  ) {
    Logger.v("setUserProperty(name=$name, value=$value)")
    userProperties[name] = value
    analytics.setUserProperty(name, value)
  }

  fun setCollectionEnabled(enabled: Boolean) {
    analytics.setAnalyticsCollectionEnabled(enabled)
    if (enabled) {
      userProperties.forEach { (name, value) -> analytics.setUserProperty(name, value) }
    }
  }
}
