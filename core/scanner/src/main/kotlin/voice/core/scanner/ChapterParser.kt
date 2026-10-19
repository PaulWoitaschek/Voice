package voice.core.scanner

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import voice.core.common.comparator.NaturalOrderComparator
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.isAudioFile
import voice.core.data.repo.ChapterRepo
import voice.core.data.repo.getOrPut
import voice.core.documentfile.CachedDocumentFile
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

internal data class ChapterParseResult(
  val chapters: List<Chapter>,
  val firstChapterMetadata: Metadata?,
)

@Inject
internal class ChapterParser(
  private val chapterRepo: ChapterRepo,
  private val mediaAnalyzer: MediaAnalyzer,
) {

  suspend fun parse(documentFile: CachedDocumentFile): ChapterParseResult = coroutineScope {
    val analyzedMetadata = ConcurrentHashMap<ChapterId, Metadata>()
    val parallelAnalyses = Semaphore(4)

    val chapters = documentFile.audioFilesInOrder()
      .map { file ->
        async {
          parallelAnalyses.withPermit {
            val id = ChapterId(file.uri)
            chapterRepo.getOrPut(
              id = id,
              lastModified = Instant.ofEpochMilli(file.lastModified),
              fileSize = file.length,
            ) {
              val metaData = mediaAnalyzer.analyze(file) ?: return@getOrPut null
              analyzedMetadata[id] = metaData
              Chapter(
                id = id,
                duration = metaData.duration,
                fileLastModified = Instant.ofEpochMilli(file.lastModified),
                name = metaData.title ?: metaData.fileName,
                markData = metaData.chapters,
                fileSize = file.length,
              )
            }
          }
        }
      }
      .awaitAll()
      .filterNotNull()
    ChapterParseResult(
      chapters = chapters,
      firstChapterMetadata = chapters.firstOrNull()?.let { analyzedMetadata[it.id] },
    )
  }
}

// Cloud providers like Nextcloud put ids into their uris instead of the file names, so the uris can't give the order.
private fun CachedDocumentFile.audioFilesInOrder(): List<CachedDocumentFile> = when {
  isDirectory ->
    children
      .sortedWith(compareBy<CachedDocumentFile> { it.isFile }.thenBy(NaturalOrderComparator.stringComparator) { it.name.orEmpty() })
      .flatMap { it.audioFilesInOrder() }
  isAudioFile() -> listOf(this)
  else -> emptyList()
}
