package de.ph1b.audiobook.data

import android.net.Uri
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

  @Suppress("SameParameterValue")
  private fun bookPosition(chapters: List<Chapter2>, currentChapter: Uri, positionInChapter: Long): Long {
    return Book2(
      content = BookContent2(
        author = UUID.randomUUID().toString(),
        name = UUID.randomUUID().toString(),
        positionInChapter = positionInChapter,
        playbackSpeed = 1F,
        addedAt = Instant.EPOCH,
        chapters = chapters.map { it.uri },
        cover = null,
        currentChapter = currentChapter,
        isActive = true,
        lastPlayedAt = Instant.EPOCH,
        skipSilence = false,
        id = Book2.Id(UUID.randomUUID().toString())
      ),
      chapters = chapters,
    ).position
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

