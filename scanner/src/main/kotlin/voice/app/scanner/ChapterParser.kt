package voice.app.scanner

import java.time.Instant
import javax.inject.Inject
import voice.data.Chapter
import voice.data.ChapterId
import voice.data.isAudioFile
import voice.data.repo.ChapterRepo
import voice.documentfile.CachedDocumentFile

class ChapterParser
@Inject constructor(
  private val chapterRepo: ChapterRepo,
  private val mediaAnalyzer: MediaAnalyzer,
) {

  suspend fun parse(documentFile: CachedDocumentFile): List<Chapter> {
    val result = mutableListOf<Chapter>()

    suspend fun parseChapters(file: CachedDocumentFile) {
      if (file.isAudioFile()) {
        val id = ChapterId(file.uri)
        val chapter = chapterRepo.getOrPut(id, Instant.ofEpochMilli(file.lastModified)) {
          val metaData = mediaAnalyzer.analyze(file) ?: return@getOrPut null
          Chapter(
            id = id,
            duration = metaData.duration,
            fileLastModified = Instant.ofEpochMilli(file.lastModified),
            name = metaData.chapterName,
            markData = metaData.chapters,
          )
        }
        if (chapter != null) {
          result.add(chapter)
        }
      } else if (file.isDirectory) {
        file.children
          .forEach {
            parseChapters(it)
          }
      }
    }

    parseChapters(file = documentFile)
    return result.sorted()
  }
}
