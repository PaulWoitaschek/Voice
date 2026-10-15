package voice.core.logging.crashlytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import voice.core.logging.api.LogWriter
import voice.core.logging.api.Logger

internal class CrashlyticsLogWriter : LogWriter {

  private val crashlytics: FirebaseCrashlytics get() = FirebaseCrashlytics.getInstance()

  override fun log(
    severity: Logger.Severity,
    message: String?,
    throwable: Throwable?,
  ) {
    if (severity <= Logger.Severity.Verbose) {
      return
    }

    crashlytics.log(crashlyticsMessage(message, throwable))
    if (severity >= Logger.Severity.Error && throwable != null) {
      crashlytics.recordException(throwable)
    }
  }
}

// exception messages carry file paths, server addresses and responses, so only the types go into the log
internal fun crashlyticsMessage(
  message: String?,
  throwable: Throwable?,
): String {
  val types = generateSequence(throwable) { it.cause }
    .take(5)
    .joinToString(separator = " caused by ") { it.javaClass.name }
  return listOfNotNull(message?.ifEmpty { null }, types.ifEmpty { null }).joinToString(separator = ": ")
}
