package de.ph1b.audiobook.misc

import de.ph1b.audiobook.data.MarkData
import de.ph1b.audiobook.ffmpeg.ffprobe
import de.ph1b.audiobook.misc.metadata.MetaDataScanResult
import de.ph1b.audiobook.misc.metadata.TagType
import de.ph1b.audiobook.misc.metadata.findTag
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonDecodingException
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.time.seconds

/**
 * Analyzes media files for meta data and duration.
 */
class MediaAnalyzer
@Inject constructor() {

  private val json = Json(JsonConfiguration.Stable.copy(ignoreUnknownKeys = true))

  suspend fun analyze(file: File): Result {
    Timber.d("analyze $file")

    val elements = ffProbeCommand(file)
    val result = ffprobe(*elements.toTypedArray())
    if (result.code != 0) {
      Timber.e("Unable to parse $file, ${result.message}")
      return Result.Failure
    }
    Timber.d(result.message)

    val parsed = try {
      json.parse(MetaDataScanResult.serializer(), result.message)
    } catch (e: JsonDecodingException) {
      Timber.e(e, "Unable to parse $file")
      return Result.Failure
    }

    val duration = parsed.format?.duration
    return if (duration != null) {
      Result.Success(
        duration = duration.seconds.toLongMilliseconds(),
        chapterName = parsed.findTag(TagType.Title) ?: chapterNameFallback(file),
        author = parsed.findTag(TagType.Artist),
        bookName = parsed.findTag(TagType.Album),
        chapters = parsed.chapters.mapIndexed { index, metaDataChapter ->
          MarkData(
            startMs = metaDataChapter.start,
            name = metaDataChapter.tags?.title ?: (index + 1).toString()
          )
        }
      )
    } else {
      Timber.e("Unable to parse $file")
      Result.Failure
    }
  }

  sealed class Result {
    data class Success(
      val duration: Long,
      val chapterName: String,
      val author: String?,
      val bookName: String?,
      val chapters: List<MarkData>
    ) : Result() {
      init {
        require(duration > 0)
      }
    }

    object Failure : Result()
  }
}

private fun ffProbeCommand(file: File): List<String> {
  return listOf(
    "-i",
    file.absolutePath,
    "-print_format",
    "json=c=1",
    "-show_chapters",
    "-loglevel",
    "quiet",
    "-show_entries",
    "format=duration",
    "-show_entries",
    "format_tags=artist,title,album",
    "-show_entries",
    "stream_tags=title,artist,album"
  )
}

private fun chapterNameFallback(file: File): String {
  val name = file.name ?: "Chapter"
  return name.substringBeforeLast(".")
    .trim()
    .takeUnless { it.isEmpty() }
    ?: name
}
