package voice.core.logging.crashlytics

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import voice.core.initializer.AppInitializer
import voice.core.logging.api.Logger

@ContributesIntoSet(AppScope::class)
class CrashlyticsLogWriterInitializer : AppInitializer {

  override fun onAppStart(application: Application) {
    Logger.install(CrashlyticsLogWriter())
  }
}
