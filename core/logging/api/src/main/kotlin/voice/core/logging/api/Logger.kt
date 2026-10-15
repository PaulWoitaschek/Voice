package voice.core.logging.api

object Logger {

  fun v(message: String) {
    log(severity = Severity.Verbose, message = message, throwable = null)
  }

  fun d(message: String) {
    log(severity = Severity.Debug, message = message, throwable = null)
  }

  fun i(message: String) {
    log(severity = Severity.Info, message = message, throwable = null)
  }

  fun w(message: String) {
    log(severity = Severity.Warn, message = message, throwable = null)
  }

  fun w(
    throwable: Throwable,
    message: String? = null,
  ) {
    log(severity = Severity.Warn, message = message, throwable = throwable)
  }

  fun nonFatal(throwable: Throwable) {
    log(severity = Severity.Error, message = null, throwable = throwable)
  }

  private var writers: Set<LogWriter> = emptySet()

  fun install(writer: LogWriter) {
    writers = writers + writer
  }

  private fun log(
    severity: Severity,
    message: String?,
    throwable: Throwable?,
  ) {
    if (message.isNullOrEmpty() && throwable == null) return
    writers.forEach {
      it.log(severity, message, throwable)
    }
  }

  enum class Severity {
    Verbose,
    Debug,
    Info,
    Warn,
    Error,
  }
}

interface LogWriter {

  fun log(
    severity: Logger.Severity,
    message: String?,
    throwable: Throwable?,
  )
}
