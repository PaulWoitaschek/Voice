package de.ph1b.audiobook.scanner

import androidx.documentfile.provider.DocumentFile
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import de.ph1b.audiobook.data.BookContent2
import de.ph1b.audiobook.data.Chapter2
import de.ph1b.audiobook.data.repo.BookContentRepo
import de.ph1b.audiobook.data.repo.ChapterRepo
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaScanner2
@Inject constructor(
  private val bookContentRepo: BookContentRepo,
  private val chapterRepo: ChapterRepo,
  private val mediaAnalyzer: MediaAnalyzer,
) {

  suspend fun scan(folders: List<DocumentFile>) {
    val allFiles = folders.flatMap { it.listFiles().toList() }
    bookContentRepo.setAllInactiveExcept(allFiles.map { it.uri })
    allFiles.forEach { scan(it) }
  }

  private suspend fun scan(file: DocumentFile) {
    val fileName = file.name ?: return
    val chapters = file.parseChapters()
    if (chapters.isEmpty()) return
    val chapterUris = chapters.map { it.uri }
    val content = bookContentRepo.getOrPut(file.uri) {
      BookContent2(
        uri = file.uri,
        isActive = true,
        addedAt = Instant.now(),
        author = mediaAnalyzer.analyze(chapterUris.first())?.author,
        lastPlayedAt = Instant.EPOCH,
        loudnessGain = 0,
        name = fileName,
        playbackSpeed = 1F,
        position = 0,
        duration = chapters.sumOf { it.duration },
        skipSilence = false,
        type = if (file.isFile) {
          BookContent2.Type.File
        } else {
          BookContent2.Type.Folder
        },
        chapters = chapterUris,
        positionInChapter = 0L,
        currentChapter = chapters.first().uri,
        cover = null
      )
    }

    val currentChapterGone = content.currentChapter !in chapterUris
    val updated = content.copy(
      duration = chapters.sumOf { it.duration },
      chapters = chapterUris,
      currentChapter = if (currentChapterGone) chapterUris.first() else content.currentChapter,
      positionInChapter = if (currentChapterGone) 0 else content.positionInChapter,
      isActive = true,
    )
    if (content != updated) {
      bookContentRepo.put(updated)
    }
  }

  private suspend fun DocumentFile.parseChapters(): List<Chapter2> {
    val result = mutableListOf<Chapter2>()
    parseChapters(file = this, result = result)
    return result
  }

  private suspend fun parseChapters(file: DocumentFile, result: MutableList<Chapter2>) {
    if (file.isFile && file.type?.startsWith("audio/") == true) {
      val chapter = chapterRepo.getOrPut(file.uri, Instant.ofEpochMilli(file.lastModified())) {
        val metaData = mediaAnalyzer.analyze(file.uri) ?: return@getOrPut null
        Chapter2(
          uri = file.uri,
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
