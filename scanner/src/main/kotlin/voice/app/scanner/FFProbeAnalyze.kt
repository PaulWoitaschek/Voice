package voice.app.scanner

import android.content.Context
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import voice.documentfile.CachedDocumentFile
import voice.ffmpeg.ffprobe
import voice.logging.core.Logger
import javax.inject.Inject

class FFProbeAnalyze
@Inject constructor(private val context: Context) {

  private val json = Json {
    ignoreUnknownKeys = true
    allowStructuredMapKeys = true
  }

  internal suspend fun analyze(file: CachedDocumentFile): MetaDataScanResult? {
    val tagKeys = TagType.entries.flatMap { it.keys }.toSet().joinToString(",")
    val result = ffprobe(
      input = file.uri,
      context = context,
      command = listOf(
        "-print_format", "json=c=1",
        "-show_chapters",
        "-loglevel", "quiet",
        "-show_entries", "format=duration",
        "-show_entries", "format_tags=$tagKeys",
        "-show_entries", "stream_tags=$tagKeys",
        // only select the audio stream
        "-select_streams", "a",
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
