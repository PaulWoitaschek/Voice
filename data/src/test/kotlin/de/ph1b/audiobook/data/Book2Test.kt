package de.ph1b.audiobook.data

import io.kotest.matchers.longs.shouldBeExactly
import org.junit.Test
import java.time.Instant
import java.util.UUID

class Book2Test {

  @Test
  fun bookPositionForSingleFile() {
    val chapter = chapter(1000)
    val position = bookPosition(chapters = listOf(chapter), currentChapter = chapter.id, positionInChapter = 500)
    position shouldBeExactly 500
  }

  @Test
  fun bookPositionForFirstChapterInMultipleFiles() {
    val chapterOne = chapter(1000)
    val chapterTwo = chapter(500)
    val position = bookPosition(chapters = listOf(chapterOne, chapterTwo), currentChapter = chapterOne.id, positionInChapter = 500)
    position shouldBeExactly 500
  }

  @Test
  fun bookPositionForLastChapterInMultipleFiles() {
    val chapterOne = chapter(1000)
    val chapterTwo = chapter(500)
    val position = bookPosition(chapters = listOf(chapterOne, chapterTwo), currentChapter = chapterTwo.id, positionInChapter = 500)
    position shouldBeExactly 1500
  }

  @Suppress("SameParameterValue")
  private fun bookPosition(chapters: List<Chapter>, currentChapter: Chapter.Id, positionInChapter: Long): Long {
    return Book(
      content = BookContent(
        author = UUID.randomUUID().toString(),
        name = UUID.randomUUID().toString(),
        positionInChapter = positionInChapter,
        playbackSpeed = 1F,
        addedAt = Instant.EPOCH,
        chapters = chapters.map { it.id },
        cover = null,
        currentChapter = currentChapter,
        isActive = true,
        lastPlayedAt = Instant.EPOCH,
        skipSilence = false,
        id = Book.Id(UUID.randomUUID().toString())
      ),
      chapters = chapters,
    ).position
  }

  private fun chapter(duration: Long): Chapter {
    return Chapter(
      id = Chapter.Id("http://${UUID.randomUUID()}"),
      duration = duration,
      fileLastModified = Instant.EPOCH,
      markData = emptyList(),
      name = "name"
    )
  }
}

