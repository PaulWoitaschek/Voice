package voice.app.scanner

import androidx.documentfile.provider.DocumentFile
import voice.common.BookId
import voice.data.Book
import voice.data.BookContent
import voice.data.Chapter
import voice.data.repo.BookContentRepo
import voice.data.repo.ChapterRepo
import voice.data.toUri
import java.time.Instant
import javax.inject.Inject

class MediaScanner
@Inject constructor(
  private val contentRepo: BookContentRepo,
  private val chapterRepo: ChapterRepo,
  private val mediaAnalyzer: MediaAnalyzer,
) {

  suspend fun scan(folders: List<DocumentFile>) {
    val allFiles = folders.flatMap { it.listFiles().toList() }
    contentRepo.setAllInactiveExcept(allFiles.map { BookId(it.uri) })
    allFiles.forEach { scan(it) }
  }

  private suspend fun scan(file: DocumentFile) {
    val chapters = file.parseChapters().sorted()
    if (chapters.isEmpty()) return
    val chapterIds = chapters.map { it.id }
    val id = BookId(file.uri)
    val content = contentRepo.getOrPut(id) {
      val analyzed = mediaAnalyzer.analyze(chapterIds.first().toUri())
      val name = analyzed?.bookName
        ?: file.name?.let { name ->
          if (file.isFile) {
            name.substringBeforeLast(".")
          } else {
            name
          }
        }
        ?: return
      val content = BookContent(
        id = id,
        isActive = true,
        addedAt = Instant.now(),
        author = analyzed?.author,
        lastPlayedAt = Instant.EPOCH,
        name = name,
        playbackSpeed = 1F,
        skipSilence = false,
        chapters = chapterIds,
        positionInChapter = 0L,
        currentChapter = chapters.first().id,
        cover = null
      )

      validateIntegrity(content, chapters)

      content
    }

    val currentChapterGone = content.currentChapter !in chapterIds
    val currentChapter = if (currentChapterGone) chapterIds.first() else content.currentChapter
    val positionInChapter = if (currentChapterGone) 0 else content.positionInChapter
    val updated = content.copy(
      chapters = chapterIds,
      currentChapter = currentChapter,
      positionInChapter = positionInChapter,
      isActive = true,
    )
    if (content != updated) {
      validateIntegrity(updated, chapters)
      contentRepo.put(updated)
    }
  }

  private fun validateIntegrity(content: BookContent, chapters: List<Chapter>) {
    // the init block performs integrity validation
    Book(content, chapters)
  }

  private suspend fun DocumentFile.parseChapters(): List<Chapter> {
    val result = mutableListOf<Chapter>()
    parseChapters(file = this, result = result)
    return result
  }

  private suspend fun parseChapters(file: DocumentFile, result: MutableList<Chapter>) {
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
          parseChapters(it, result)
        }
    }
  }
}
