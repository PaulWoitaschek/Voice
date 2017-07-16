package de.ph1b.audiobook.features.chapterReader

import org.assertj.core.api.Assertions.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MatroskaChapterReadingTest {
  private val testFile = File(javaClass.classLoader.getResource("matroskaChapterReader/simple.mka").path)
  private val testFileChapters = listOf(
      MatroskaChapter(0, listOf(
          MatroskaChapterName("Part 1", setOf("eng"))
      ), listOf()),
      MatroskaChapter(2000000000L, listOf(
          MatroskaChapterName("Part 2", setOf("eng"))
      ), listOf(
          MatroskaChapter(2000000000L, listOf(
              MatroskaChapterName("Podczęść 1", setOf("pol")),
              MatroskaChapterName("Subpart 1", setOf("eng", "ger"))
          ), listOf()),
          MatroskaChapter(3000000000L, listOf(
              MatroskaChapterName("Subpart 2", setOf("eng"))
          ), listOf()),
          MatroskaChapter(4000000000L, listOf(), listOf())
      ))
  )

  @Test
  fun readMatroskaChaptersTest() {
    val chapters = readMatroskaChapters(testFile)
    assertThat(chapters).isEqualTo(testFileChapters)
  }

  @Test
  fun matroskaChapterGetNameTest() {
    assertThat(MatroskaChapter(0L, listOf(), listOf()).getName()).isNull()
    val chapter = MatroskaChapter(0L, listOf(
        MatroskaChapterName("Podczęść 1", setOf("pol")),
        MatroskaChapterName("Subpart 1", setOf("eng", "ger")),
        MatroskaChapterName("サブパート1", setOf("jpn"))
    ), listOf())
    assertThat(chapter.getName()).isEqualTo("Podczęść 1")
    assertThat(chapter.getName("ger", "jpn")).isEqualTo("Subpart 1")
    assertThat(chapter.getName("ind", "kac", "jpn", "eng")).isEqualTo("サブパート1")
  }
}
