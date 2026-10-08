package voice.features.support

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import voice.core.initializer.AppInitializer

// runs even while the support card is off, as Google Play refunds purchases that aren't acknowledged within three days
@ContributesIntoSet(AppScope::class)
class SyncSupportOnAppStart(private val backend: SupportBackend) : AppInitializer {

  override fun onAppStart(application: Application) {
    backend.refresh()
  }
}
