package voice.core.data

import kotlin.test.Test
import kotlin.test.assertEquals

class BookTest {

  @Test
  fun bookPositionForSingleFile() {
    val chapter = chapter(1000)
    val position = bookPosition(chapters = listOf(chapter), currentChapter = chapter.id, positionInChapter = 500)
    assertEquals(expected = 500, actual = position)
  }

  @Test
  fun bookPositionForFirstChapterInMultipleFiles() {
    val chapterOne = chapter(1000)
    val chapterTwo = chapter(500)
    val position = bookPosition(chapters = listOf(chapterOne, chapterTwo), currentChapter = chapterOne.id, positionInChapter = 500)
    assertEquals(expected = 500, actual = position)
  }

  @Test
  fun bookPositionForLastChapterInMultipleFiles() {
    val chapterOne = chapter(1000)
    val chapterTwo = chapter(500)
    val position = bookPosition(chapters = listOf(chapterOne, chapterTwo), currentChapter = chapterTwo.id, positionInChapter = 500)
    assertEquals(expected = 1500, actual = position)
  }

  @Test
  fun globalPositionWhenTimeIs0AndCurrentFileIsFirst() {
    val chapters = listOf(chapter(duration = 12345), chapter())
    val book = book(
      time = 0,
      chapters = chapters,
      currentChapter = chapters.first().id,
    )
    assertEquals(expected = 0L, actual = book.position)
  }

  @Test
  fun globalPositionWhenTimeIsNot0AndCurrentFileIsFirst() {
    val chapters = listOf(chapter(duration = 12345), chapter())
    val book = book(
      time = 23,
      chapters = chapters,
      currentChapter = chapters.first().id,
    )
    assertEquals(expected = 23, actual = book.position)
  }

  @Test
  fun globalPositionWhenTimeIs0AndCurrentFileIsNotFirst() {
    val lastChapterId = ChapterId("lastChapter")
    val book = book(
      time = 0,
      chapters = listOf(
        chapter(duration = 123),
        chapter(duration = 234),
        chapter(duration = 345),
        chapter(duration = 456, id = lastChapterId),
      ),
      currentChapter = lastChapterId,
    )
    assertEquals(expected = 123 + 234 + 345, actual = book.position)
  }

  @Test
  fun globalPositionWhenTimeIsNot0AndCurrentFileIsNotFirst() {
    val targetChapter = ChapterId("target")
    val book = book(
      time = 23,
      chapters = listOf(
        chapter(duration = 123),
        chapter(duration = 234),
        chapter(duration = 345, id = targetChapter),
        chapter(duration = 456),
      ),
      currentChapter = targetChapter,
    )
    assertEquals(expected = 123 + 234 + 23, actual = book.position)
  }

  @Test
  fun totalDuration() {
    val book = book(
      chapters = listOf(
        chapter(duration = 123),
        chapter(duration = 234),
        chapter(duration = 345),
        chapter(duration = 456),
      ),
    )

    assertEquals(expected = 123 + 234 + 345 + 456, actual = book.duration)
  }

  @Test
  fun currentChapter() {
    val ch1 = chapter()
    val ch2 = chapter()
    val ch3 = chapter()
    val book = book(
      chapters = listOf(ch1, ch2, ch3),
      currentChapter = ch2.id,
    )
    assertEquals(expected = ch2, actual = book.currentChapter)
  }

  @Test
  fun currentChapterIndex() {
    val ch1 = chapter()
    val ch2 = chapter()
    val ch3 = chapter()
    val book = book(
      chapters = listOf(ch1, ch2, ch3),
      currentChapter = ch2.id,
    )

    assertEquals(expected = 1, actual = book.content.currentChapterIndex)
  }

  @Test
  fun elapsedPositionStaysInCurrentChapter() {
    val chapter = chapter(duration = 1000)
    val book = book(
      chapters = listOf(chapter),
      time = 200,
      currentChapter = chapter.id,
    )

    val updated = book.withElapsedPosition(elapsedTime = 100, playbackSpeed = 1F)

    assertEquals(expected = chapter.id, actual = updated.content.currentChapter)
    assertEquals(expected = 300, actual = updated.content.positionInChapter)
  }

  @Test
  fun elapsedPositionMovesIntoNextChapter() {
    val firstChapter = chapter(duration = 1000)
    val secondChapter = chapter(duration = 500)
    val book = book(
      chapters = listOf(firstChapter, secondChapter),
      time = 900,
      currentChapter = firstChapter.id,
    )

    val updated = book.withElapsedPosition(elapsedTime = 200, playbackSpeed = 1F)

    assertEquals(expected = secondChapter.id, actual = updated.content.currentChapter)
    assertEquals(expected = 100, actual = updated.content.positionInChapter)
  }

  @Test
  fun elapsedPositionUsesPlaybackSpeed() {
    val firstChapter = chapter(duration = 1000)
    val secondChapter = chapter(duration = 500)
    val book = book(
      chapters = listOf(firstChapter, secondChapter),
      time = 900,
      currentChapter = firstChapter.id,
    )

    val updated = book.withElapsedPosition(elapsedTime = 100, playbackSpeed = 1.5F)

    assertEquals(expected = secondChapter.id, actual = updated.content.currentChapter)
    assertEquals(expected = 50, actual = updated.content.positionInChapter)
  }

  @Test
  fun elapsedPositionUsesNextChapterAtBoundary() {
    val firstChapter = chapter(duration = 1000)
    val secondChapter = chapter(duration = 500)
    val book = book(
      chapters = listOf(firstChapter, secondChapter),
      time = 900,
      currentChapter = firstChapter.id,
    )

    val updated = book.withElapsedPosition(elapsedTime = 100, playbackSpeed = 1F)

    assertEquals(expected = secondChapter.id, actual = updated.content.currentChapter)
    assertEquals(expected = 0, actual = updated.content.positionInChapter)
  }

  @Test
  fun elapsedPositionClampsAtBookEnd() {
    val firstChapter = chapter(duration = 1000)
    val secondChapter = chapter(duration = 500)
    val book = book(
      chapters = listOf(firstChapter, secondChapter),
      time = 900,
      currentChapter = firstChapter.id,
    )

    val updated = book.withElapsedPosition(elapsedTime = 1000, playbackSpeed = 1F)

    assertEquals(expected = secondChapter.id, actual = updated.content.currentChapter)
    assertEquals(expected = secondChapter.duration, actual = updated.content.positionInChapter)
  }

  @Suppress("SameParameterValue")
  private fun bookPosition(
    chapters: List<Chapter>,
    currentChapter: ChapterId,
    positionInChapter: Long,
  ): Long {
    return book(
      chapters = chapters,
      time = positionInChapter,
      currentChapter = currentChapter,
    ).position
  }
}
