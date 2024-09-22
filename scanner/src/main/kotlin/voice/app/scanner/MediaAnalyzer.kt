package voice.app.scanner

import voice.data.MarkData
import voice.documentfile.CachedDocumentFile
import voice.documentfile.nameWithoutExtension
import voice.logging.core.Logger
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class MediaAnalyzer
@Inject constructor(private val ffProbeAnalyze: FFProbeAnalyze) {

  suspend fun analyze(file: CachedDocumentFile): Metadata? {
    val result = ffProbeAnalyze.analyze(file) ?: return null
    val duration = result.format?.duration
    return if (duration != null && duration > 0 && result.streams.isNotEmpty()) {
      Metadata(
        duration = duration.seconds.inWholeMilliseconds,
        fileName = file.nameWithoutExtension(),
        artist = result.findTag(TagType.Artist),
        album = result.findTag(TagType.Album),
        title = result.findTag(TagType.Title),
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
    val artist: String?,
    val album: String?,
    val title: String?,
    val fileName: String,
    val chapters: List<MarkData>,
  )
}
