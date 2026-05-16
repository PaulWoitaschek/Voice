package voice.core.scanner

import dev.zacsweers.metro.Inject
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.ChapterRepo
import voice.core.data.repo.getOrPut
import voice.core.documentfile.CachedDocumentFile
import java.time.Instant

internal class ChapterParseResult(
  val chapters: List<Chapter>,
  val firstFileMetadata: Metadata?,
)

@Inject
internal class ChapterParser(
  private val chapterRepo: ChapterRepo,
  private val mediaAnalyzer: MediaAnalyzer,
) {

  suspend fun parse(audioFiles: List<CachedDocumentFile>): ChapterParseResult {
    val result = mutableListOf<Chapter>()
    val analyzedMetadata = mutableMapOf<ChapterId, Metadata>()

    audioFiles.forEach { file ->
      val id = ChapterId(file.uri)
      val chapter = chapterRepo.getOrPut(id, Instant.ofEpochMilli(file.lastModified)) {
        val metaData = mediaAnalyzer.analyze(file) ?: return@getOrPut null
        analyzedMetadata[id] = metaData
        Chapter(
          id = id,
          duration = metaData.duration,
          fileLastModified = Instant.ofEpochMilli(file.lastModified),
          name = metaData.title ?: metaData.fileName,
          markData = metaData.chapters,
        )
      }
      if (chapter != null) {
        result.add(chapter)
      }
    }

    val sorted = result.sorted()
    val firstFileMetadata = sorted.firstOrNull()?.let { analyzedMetadata[it.id] }
    return ChapterParseResult(sorted, firstFileMetadata)
  }
}
