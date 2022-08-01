package voice.app.scanner

import androidx.documentfile.provider.DocumentFile
import voice.data.Chapter
import voice.data.repo.ChapterRepo
import java.time.Instant
import javax.inject.Inject

class ChapterParser
@Inject constructor(
  private val chapterRepo: ChapterRepo,
  private val mediaAnalyzer: MediaAnalyzer,
) {

  suspend fun parse(documentFile: DocumentFile): List<Chapter> {
    val result = mutableListOf<Chapter>()

    suspend fun parseChapters(file: DocumentFile) {
      val mimeType = file.type
      if (
        file.isFile &&
        mimeType != null &&
        (mimeType.startsWith("audio/") || mimeType.startsWith("video/"))
      ) {
        val id = Chapter.Id(file.uri)
        val chapter = chapterRepo.getOrPut(id, Instant.ofEpochMilli(file.lastModified())) {
          val metaData = mediaAnalyzer.analyze(file.uri) ?: return@getOrPut null
          Chapter(
            id = id,
            duration = metaData.duration,
            fileLastModified = Instant.ofEpochMilli(file.lastModified()),
            name = metaData.chapterName,
            markData = metaData.chapters
          )
        }
        if (chapter != null) {
          result.add(chapter)
        }
      } else if (file.isDirectory) {
        file.listFiles()
          .forEach {
            parseChapters(it)
          }
      }
    }

    parseChapters(file = documentFile)
    return result.sorted()
  }
}
