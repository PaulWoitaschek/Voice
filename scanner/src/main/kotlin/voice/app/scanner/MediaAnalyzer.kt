package voice.app.scanner

import androidx.documentfile.provider.DocumentFile
import voice.data.MarkData
import voice.logging.core.Logger
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class MediaAnalyzer
@Inject constructor(
  private val ffProbeAnalyze: FFProbeAnalyze,
) {

  suspend fun analyze(file: DocumentFile): Metadata? {
    val result = ffProbeAnalyze.analyze(file) ?: return null
    val duration = result.format?.duration
    return if (duration != null && duration > 0 && result.streams.isNotEmpty()) {
      Metadata(
        duration = duration.seconds.inWholeMilliseconds,
        chapterName = result.findTag(TagType.Title) ?: file.chapterNameFallback(),
        author = result.findTag(TagType.Artist),
        bookName = result.findTag(TagType.Album),
        chapters = result.chapters.mapIndexed { index, metaDataChapter ->
          MarkData(
            startMs = metaDataChapter.start.inWholeMilliseconds,
            name = metaDataChapter.tags?.find(TagType.Title) ?: (index + 1).toString(),
          )
        },
      )
    } else {
      Logger.w("Unable to parse ${file.uri}")
      null
    }
  }

  data class Metadata(
    val duration: Long,
    val chapterName: String?,
    val author: String?,
    val bookName: String?,
    val chapters: List<MarkData>,
  )
}

private fun DocumentFile.chapterNameFallback(): String? {
  val name = name ?: return null
  return name.substringBeforeLast(".")
    .trim()
    .takeUnless { it.isEmpty() }
    ?: name
}
