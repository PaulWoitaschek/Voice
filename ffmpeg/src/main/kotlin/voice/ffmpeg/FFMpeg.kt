package voice.ffmpeg

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.LogRedirectionStrategy
import com.arthenica.ffmpegkit.SessionState
import kotlinx.coroutines.suspendCancellableCoroutine
import voice.logging.core.Logger
import kotlin.coroutines.resume

suspend fun ffprobe(input: Uri, context: Context, command: List<String>): String? {
  FFmpegKitConfig.setLogRedirectionStrategy(LogRedirectionStrategy.NEVER_PRINT_LOGS)
  val fullCommand = fullCommand(input, context, command) ?: return null
  return suspendCancellableCoroutine { cont ->
    val probeSession = FFprobeKit.executeWithArgumentsAsync(fullCommand.toTypedArray()) { session ->
      when (session.state) {
        SessionState.COMPLETED -> {
          cont.resume(session.output)
        }
        SessionState.FAILED -> {
          cont.resume(null)
        }
        else -> {}
      }
    }
    cont.invokeOnCancellation { probeSession.cancel() }
  }
}

suspend fun ffmpeg(input: Uri, context: Context, command: List<String>): String? {
  FFmpegKitConfig.setLogRedirectionStrategy(LogRedirectionStrategy.NEVER_PRINT_LOGS)
  val fullCommand = fullCommand(input, context, command) ?: return null
  return suspendCancellableCoroutine { cont ->
    val probeSession = FFmpegKit.executeWithArgumentsAsync(fullCommand.toTypedArray()) { session ->
      when (session.state) {
        SessionState.COMPLETED -> {
          cont.resume(session.output)
        }
        SessionState.FAILED -> {
          cont.resume(null)
        }
        else -> {}
      }
    }
    cont.invokeOnCancellation { probeSession.cancel() }
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
