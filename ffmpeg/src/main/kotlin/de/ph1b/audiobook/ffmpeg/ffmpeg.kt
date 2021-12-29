package de.ph1b.audiobook.ffmpeg

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.SessionState
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun ffprobe(vararg command: String): FfmpegCommandResult = suspendCancellableCoroutine { cont ->
  val probeSession = FFprobeKit.executeAsync(command) { session ->
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

suspend fun ffmpeg(vararg command: String): FfmpegCommandResult = suspendCancellableCoroutine { cont ->
  val probeSession = FFmpegKit.executeAsync(command) { session ->
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

data class FfmpegCommandResult(
  val message: String,
  val success: Boolean
)
