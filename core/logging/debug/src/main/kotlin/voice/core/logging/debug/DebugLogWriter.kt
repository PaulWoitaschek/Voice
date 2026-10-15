package voice.core.logging.debug

import android.util.Log
import voice.core.logging.api.LogWriter
import voice.core.logging.api.Logger
import java.io.PrintWriter
import java.io.StringWriter
import java.util.regex.Pattern

internal class DebugLogWriter : LogWriter {

  /**
   * This logic was borrowed from Timber: https://github.com/JakeWharton/timber
   */
  override fun log(
    severity: Logger.Severity,
    message: String?,
    throwable: Throwable?,
  ) {
    val text = listOfNotNull(message?.ifEmpty { null }, throwable?.let(::getStackTraceString))
      .joinToString(separator = "\n")
    val tag = Throwable().stackTrace
      .first { it.className !in fqcnIgnore }
      .let(::createStackElementTag)
    val priority = severity.priority
    if (text.length < MAX_LOG_LENGTH) {
      if (priority == Log.ASSERT) {
        Log.wtf(tag, text)
      } else {
        Log.println(priority, tag, text)
      }
      return
    }

    // Split by line, then ensure each line can fit into Log's maximum length.
    var i = 0
    val length = text.length
    while (i < length) {
      var newline = text.indexOf('\n', i)
      newline = if (newline != -1) newline else length
      do {
        val end = newline.coerceAtMost(i + MAX_LOG_LENGTH)
        val part = text.substring(i, end)
        if (priority == Log.ASSERT) {
          Log.wtf(tag, part)
        } else {
          Log.println(priority, tag, part)
        }
        i = end
      } while (i < newline)
      i++
    }
  }
}

private val fqcnIgnore = listOf(
  DebugLogWriter::class.java.name,
  Logger::class.java.name,
)

private fun getStackTraceString(t: Throwable): String {
  // Don't replace this with Log.getStackTraceString() - it hides
  // UnknownHostException, which is not what we want.
  val sw = StringWriter(256)
  val pw = PrintWriter(sw, false)
  t.printStackTrace(pw)
  pw.flush()
  return sw.toString()
}

private const val MAX_LOG_LENGTH = 4000
private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")

private fun createStackElementTag(element: StackTraceElement): String {
  var tag = element.className.substringAfterLast('.')
  val matcher = ANONYMOUS_CLASS.matcher(tag)
  if (matcher.find()) {
    tag = matcher.replaceAll("")
  }
  return "$tag:${element.lineNumber}"
}

private val Logger.Severity.priority: Int
  get() = when (this) {
    Logger.Severity.Verbose -> Log.VERBOSE
    Logger.Severity.Debug -> Log.DEBUG
    Logger.Severity.Info -> Log.INFO
    Logger.Severity.Warn -> Log.WARN
    Logger.Severity.Error -> Log.ERROR
  }
