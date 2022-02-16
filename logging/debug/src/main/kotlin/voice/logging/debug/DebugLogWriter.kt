package voice.logging.debug

import android.os.Build
import android.util.Log
import voice.logging.core.LogWriter
import voice.logging.core.Logger
import java.util.regex.Pattern

internal class DebugLogWriter : LogWriter {

  /**
   * This logic was borrowed from Timber: https://github.com/JakeWharton/timber
   */
  override fun log(severity: Logger.Severity, message: String, throwable: Throwable?) {
    val tag = Throwable().stackTrace
      .first { it.className !in fqcnIgnore }
      .let(::createStackElementTag)
    val priority = severity.priority
    if (message.length < MAX_LOG_LENGTH) {
      if (priority == Log.ASSERT) {
        Log.wtf(tag, message)
      } else {
        Log.println(priority, tag, message)
      }
      return
    }

    // Split by line, then ensure each line can fit into Log's maximum length.
    var i = 0
    val length = message.length
    while (i < length) {
      var newline = message.indexOf('\n', i)
      newline = if (newline != -1) newline else length
      do {
        val end = newline.coerceAtMost(i + MAX_LOG_LENGTH)
        val part = message.substring(i, end)
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

private const val MAX_LOG_LENGTH = 4000
private const val MAX_TAG_LENGTH = 23
private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")

private fun createStackElementTag(element: StackTraceElement): String {
  var tag = element.className.substringAfterLast('.')
  val matcher = ANONYMOUS_CLASS.matcher(tag)
  if (matcher.find()) {
    tag = matcher.replaceAll("")
  }
  // Tag length limit was removed in API 26.
  val trimmedTag = if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= 26) {
    tag
  } else {
    tag.substring(0, MAX_TAG_LENGTH)
  }
  return "$trimmedTag:${element.lineNumber}"
}

private val Logger.Severity.priority: Int
  get() = when (this) {
    Logger.Severity.Verbose -> Log.VERBOSE
    Logger.Severity.Debug -> Log.DEBUG
    Logger.Severity.Info -> Log.INFO
    Logger.Severity.Warn -> Log.WARN
    Logger.Severity.Error -> Log.ERROR
  }
