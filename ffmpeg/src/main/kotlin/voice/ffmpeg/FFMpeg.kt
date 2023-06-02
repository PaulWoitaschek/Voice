package voice.ffmpeg

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.LogRedirectionStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import voice.logging.core.Logger

private val mutex = Mutex()

suspend fun ffprobe(input: Uri, context: Context, command: List<String>): String? = mutex.withLock {
  FFmpegKitConfig.setLogRedirectionStrategy(LogRedirectionStrategy.NEVER_PRINT_LOGS)
  val fullCommand = fullCommand(input, context, command) ?: return null
  return withContext(Dispatchers.IO) {
    FFprobeKit.executeWithArguments(fullCommand.toTypedArray()).output
  }
}

suspend fun ffmpeg(input: Uri, context: Context, command: List<String>): String? = mutex.withLock {
  FFmpegKitConfig.setLogRedirectionStrategy(LogRedirectionStrategy.NEVER_PRINT_LOGS)
  val fullCommand = fullCommand(input, context, command) ?: return null
  return withContext(Dispatchers.IO) {
    FFmpegKit.executeWithArguments(fullCommand.toTypedArray()).output
  }
}

private fun fullCommand(input: Uri, context: Context, command: List<String>): List<String>? {
  val mappedInput = if (input.scheme == "content") {
    try {
      FFmpegKitConfig.getSafParameterForRead(context, input)
    } catch (e: Exception) {
      Logger.e(e, "Could not get saf parameter for $input")
      return null
    }
  } else {
    input.toFile().absolutePath
  }
  return listOf("-i", mappedInput) + command
}
