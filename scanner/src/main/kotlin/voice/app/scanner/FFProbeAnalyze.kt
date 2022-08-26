package voice.app.scanner

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import voice.ffmpeg.ffprobe
import voice.logging.core.Logger
import javax.inject.Inject

class FFProbeAnalyze
@Inject constructor(
  private val context: Context,
) {

  private val json = Json {
    ignoreUnknownKeys = true
    allowStructuredMapKeys = true
  }

  suspend fun analyze(file: DocumentFile): MetaDataScanResult? {
    val result = ffprobe(
      input = file.uri,
      context = context,
      command = listOf(
        "-print_format", "json=c=1",
        "-show_chapters",
        "-loglevel", "quiet",
        "-show_entries", "format=duration",
        "-show_entries", "format_tags=artist,title,album",
        "-show_entries", "stream_tags=artist,title,album",
        "-select_streams", "a", // only select the audio stream
      ),
    )
    if (result == null) {
      Logger.w("Unable to parse $file.")
      return null
    }

    return try {
      json.decodeFromString(MetaDataScanResult.serializer(), result)
    } catch (e: SerializationException) {
      Logger.w(e, "Unable to parse $file")
      return null
    }
  }
}
