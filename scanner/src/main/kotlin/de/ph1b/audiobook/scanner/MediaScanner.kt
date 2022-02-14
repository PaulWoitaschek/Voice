package de.ph1b.audiobook.scanner

import androidx.documentfile.provider.DocumentFile
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.repo.BookContentRepo
import de.ph1b.audiobook.data.repo.ChapterRepo
import de.ph1b.audiobook.data.toUri
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
    contentRepo.setAllInactiveExcept(allFiles.map { Book.Id(it.uri) })
    allFiles.forEach { scan(it) }
  }

  private suspend fun scan(file: DocumentFile) {
    val fileName = file.name ?: return
    val chapters = file.parseChapters()
    if (chapters.isEmpty()) return
    val chapterIds = chapters.map { it.id }
    val id = Book.Id(file.uri)
    val content = contentRepo.getOrPut(id) {
      val content = BookContent(
        id = id,
        isActive = true,
        addedAt = Instant.now(),
        author = mediaAnalyzer.analyze(chapterIds.first().toUri())?.author,
        lastPlayedAt = Instant.EPOCH,
        name = fileName,
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
    if (file.isFile && file.type?.startsWith("audio/") == true) {
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
      file.listFiles().sortedWith(NaturalOrderComparator.documentFileComparator)
        .forEach {
          parseChapters(it, result)
        }
    }
  }
}
