package voice.app.scanner

import android.content.Context
import android.net.Uri
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import voice.data.MarkData
import voice.ffmpeg.ffprobe
import voice.logging.core.Logger
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Analyzes media files for meta data and duration.
 */
class MediaAnalyzer
@Inject constructor(
  private val context: Context,
) {

  private val json = Json {
    ignoreUnknownKeys = true
    allowStructuredMapKeys = true
  }

  suspend fun analyze(uri: Uri): Metadata? {
    Logger.d("analyze $uri")

    val result = ffprobe(
      input = uri,
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
      Logger.w("Unable to parse $uri.")
      return null
    }

    val parsed = try {
      json.decodeFromString(MetaDataScanResult.serializer(), result)
    } catch (e: SerializationException) {
      Logger.w(e, "Unable to parse $uri")
      return null
    }

    val duration = parsed.format?.duration
    return if (duration != null && duration > 0) {
      Metadata(
        duration = duration.seconds.inWholeMilliseconds,
        chapterName = parsed.findTag(TagType.Title) ?: chapterNameFallback(uri),
        author = parsed.findTag(TagType.Artist),
        bookName = parsed.findTag(TagType.Album),
        chapters = parsed.chapters.mapIndexed { index, metaDataChapter ->
          MarkData(
            startMs = metaDataChapter.start.inWholeMilliseconds,
            name = metaDataChapter.tags?.find(TagType.Title) ?: (index + 1).toString(),
          )
        },
      )
    } else {
      Logger.w("Unable to parse $uri")
      null
    }
  }

  data class Metadata(
    val duration: Long,
    val chapterName: String,
    val author: String?,
    val bookName: String?,
    val chapters: List<MarkData>,
  )
}

private fun chapterNameFallback(file: Uri): String {
  val name = file.lastPathSegment ?: "Chapter"
  return name.substringBeforeLast(".")
    .trim()
    .takeUnless { it.isEmpty() }
    ?: name
}
