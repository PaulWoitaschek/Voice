package voice.core.analytics.noop

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import voice.core.analytics.api.Analytics
import voice.core.logging.api.Logger

@Inject
@ContributesBinding(AppScope::class)
class NoOpAnalytics : Analytics {

  override fun screenView(screenName: String) {
    Logger.v("screenView($screenName)")
  }

  override fun event(
    name: String,
    params: Map<String, String>,
  ) {
    Logger.v("event(name=$name, params=$params)")
  }
}
