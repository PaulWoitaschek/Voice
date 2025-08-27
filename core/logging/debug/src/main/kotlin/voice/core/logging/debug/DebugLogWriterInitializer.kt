package voice.core.logging.debug

import android.content.Context
import androidx.startup.Initializer
import voice.core.logging.core.Logger

@Suppress("unused")
class DebugLogWriterInitializer : Initializer<Unit> {

  override fun create(context: Context) {
    Logger.install(DebugLogWriter())
  }

  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
