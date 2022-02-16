package voice.logging.core

import java.io.PrintWriter
import java.io.StringWriter

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

  fun w(throwable: Throwable, message: String? = null) {
    log(severity = Severity.Warn, message = message, throwable = throwable)
  }

  fun e(throwable: Throwable, message: String) {
    log(severity = Severity.Error, message = message, throwable = throwable)
  }

  private var writers: Set<LogWriter> = emptySet()

  fun install(writer: LogWriter) {
    writers = writers + writer
  }

  /**
   * This logic is borrowed from Timber: https://github.com/JakeWharton/timber
   */
  private fun log(severity: Severity, message: String?, throwable: Throwable?) {
    var messageResult = message
    if (messageResult.isNullOrEmpty()) {
      if (throwable == null) {
        return // Swallow message if it's null and there's no throwable.
      }
      messageResult = getStackTraceString(throwable)
    } else {
      if (throwable != null) {
        messageResult += "\n" + getStackTraceString(throwable)
      }
    }

    writers.forEach {
      it.log(severity, messageResult, throwable)
    }
  }

  private fun getStackTraceString(t: Throwable): String {
    // Don't replace this with Log.getStackTraceString() - it hides
    // UnknownHostException, which is not what we want.
    val sw = StringWriter(256)
    val pw = PrintWriter(sw, false)
    t.printStackTrace(pw)
    pw.flush()
    return sw.toString()
  }

  enum class Severity {
    Verbose,
    Debug,
    Info,
    Warn,
    Error
  }
}

interface LogWriter {

  fun log(severity: Logger.Severity, message: String, throwable: Throwable?)
}
