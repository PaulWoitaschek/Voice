package voice.core.logging.crashlytics

import android.content.Context
import androidx.startup.Initializer
import voice.core.logging.core.Logger

@Suppress("unused")
class CrashlyticsLogWriterInitializer : Initializer<Unit> {

  override fun create(context: Context) {
    Logger.install(CrashlyticsLogWriter())
  }

  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
