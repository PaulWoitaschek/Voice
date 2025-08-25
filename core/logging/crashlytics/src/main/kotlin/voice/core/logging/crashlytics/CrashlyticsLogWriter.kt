package voice.core.logging.crashlytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import voice.core.logging.core.LogWriter
import voice.core.logging.core.Logger

internal class CrashlyticsLogWriter : LogWriter {

  private val crashlytics: FirebaseCrashlytics get() = FirebaseCrashlytics.getInstance()

  override fun log(
    severity: Logger.Severity,
    message: String,
    throwable: Throwable?,
  ) {
    if (severity <= Logger.Severity.Verbose) {
      return
    }

    crashlytics.log(message)
    if (severity >= Logger.Severity.Error) {
      crashlytics.recordException(throwable ?: AssertionError(message))
    }
  }
}
