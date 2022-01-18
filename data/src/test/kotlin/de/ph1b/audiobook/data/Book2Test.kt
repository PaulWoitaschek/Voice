package de.ph1b.audiobook.data

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.longs.shouldBeExactly
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class Book2Test {

  @Test
  fun bookPositionForSingleFile() {
    val chapter = chapter(1000)
    val position = bookPosition(chapters = listOf(chapter), currentChapter = chapter.uri, positionInChapter = 500)
    position shouldBeExactly 500
  }

  @Test
  fun bookPositionForFirstChapterInMultipleFiles() {
    val chapterOne = chapter(1000)
    val chapterTwo = chapter(500)
    val position = bookPosition(chapters = listOf(chapterOne, chapterTwo), currentChapter = chapterOne.uri, positionInChapter = 500)
    position shouldBeExactly 500
  }

  @Test
  fun bookPositionForLastChapterInMultipleFiles() {
    val chapterOne = chapter(1000)
    val chapterTwo = chapter(500)
    val position = bookPosition(chapters = listOf(chapterOne, chapterTwo), currentChapter = chapterTwo.uri, positionInChapter = 500)
    position shouldBeExactly 1500
  }

  private fun chapter(duration: Long): Chapter2 {
    return Chapter2(
      uri = "http://${UUID.randomUUID()}".toUri(),
      duration = duration,
      fileLastModified = Instant.EPOCH,
      markData = emptyList(),
      name = "name"
    )
  }
}
