package voice.core.logging.crashlytics

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import voice.core.initializer.AppInitializer
import voice.core.logging.core.Logger

@ContributesIntoSet(AppScope::class)
@Inject
class CrashlyticsLogWriterInitializer : AppInitializer {

  override fun onAppStart(application: Application) {
    Logger.install(CrashlyticsLogWriter())
  }
}
