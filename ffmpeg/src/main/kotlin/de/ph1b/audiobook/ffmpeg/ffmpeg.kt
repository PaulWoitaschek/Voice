package de.ph1b.audiobook.ffmpeg

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.SessionState
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun ffprobe(input: Uri, context: Context, command: List<String>): FfmpegCommandResult = suspendCancellableCoroutine { cont ->
  val probeSession = FFprobeKit.executeWithArgumentsAsync(fullCommand(input, context, command).toTypedArray()) { session ->
    when (session.state) {
      SessionState.COMPLETED -> {
        cont.resume(FfmpegCommandResult(session.output, success = true))
      }
      SessionState.FAILED -> {
        cont.resume(FfmpegCommandResult(session.output, success = false))
      }
      else -> {}
    }
  }
  cont.invokeOnCancellation { probeSession.cancel() }
}

suspend fun ffmpeg(input: Uri, context: Context, command: List<String>): FfmpegCommandResult = suspendCancellableCoroutine { cont ->
  val probeSession = FFmpegKit.executeWithArgumentsAsync(fullCommand(input, context, command).toTypedArray()) { session ->
    when (session.state) {
      SessionState.COMPLETED -> {
        cont.resume(FfmpegCommandResult(session.output, success = true))
      }
      SessionState.FAILED -> {
        cont.resume(FfmpegCommandResult(session.output, success = false))
      }
      else -> {}
    }
  }
  cont.invokeOnCancellation { probeSession.cancel() }
}

private fun fullCommand(input: Uri, context: Context, command: List<String>): List<String> {
  val mappedInput = if (input.scheme == "content") {
    FFmpegKitConfig.getSafParameterForRead(context, input)
  } else {
    input.toFile().absolutePath
  }
  return listOf("-i", mappedInput) + command
}

data class FfmpegCommandResult(
  val message: String,
  val success: Boolean
)
