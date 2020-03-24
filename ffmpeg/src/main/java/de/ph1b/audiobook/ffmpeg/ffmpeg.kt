package de.ph1b.audiobook.ffmpeg

import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.FFprobe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * As ffmpeg doesn't properly handle command return values, we need to synchronize the access so we have no races.
 */
private val mutex = Mutex()

suspend fun ffprobe(vararg command: String): FfmpegCommandResult = withDispatcherAndLock {
  val code = FFprobe.execute(command)
  val message = Config.getLastCommandOutput()
  FfmpegCommandResult(message, code)
}

suspend fun ffmpeg(vararg command: String): FfmpegCommandResult = withDispatcherAndLock {
  val code = FFmpeg.execute(command)
  val message = Config.getLastCommandOutput()
  FfmpegCommandResult(message, code)
}

private suspend inline fun <T> withDispatcherAndLock(crossinline action: () -> T): T {
  return withContext(Dispatchers.Default) {
    mutex.withLock {
      action()
    }
  }
}

data class FfmpegCommandResult(
  val message: String,
  val code: Int
)
