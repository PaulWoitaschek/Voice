package de.ph1b.audiobook.features.chapterReader

import de.ph1b.audiobook.features.chapterReader.matroska.MatroskaChapter
import de.ph1b.audiobook.features.chapterReader.matroska.MatroskaChapterName
import de.ph1b.audiobook.features.chapterReader.matroska.MatroskaChapterReader
import de.ph1b.audiobook.features.chapterReader.matroska.flattenToSparseArray
import de.ph1b.audiobook.features.chapterReader.matroska.readMatroskaChapters
import de.ph1b.audiobook.misc.toMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class MatroskaChapterReadingTest {
  private val testFile = File(javaClass.classLoader.getResource("matroskaChapterReader/simple.mka").path)
  private val testFileMatroskaChapters = listOf(
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
  private val testFileChapters = mapOf(
      0 to "Part 1",
      2000 to "Part 2",
      2001 to "+ Podczęść 1",
      3000 to "+ Subpart 2",
      4000 to "+ Chapter 3"
  )

  @Test
  fun readMatroskaChaptersTest() {
    val chapters = readMatroskaChapters(testFile)
    assertThat(chapters).isEqualTo(testFileMatroskaChapters)
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

  @Test
  fun flattenMatroskaChapterListTest() {
    assertThat(testFileMatroskaChapters
        .flattenToSparseArray("pol", "eng")
        .toMap()).isEqualTo(testFileChapters)
  }

  @Test
  fun readChapters() {
    Locale.setDefault(Locale("pol", "PL", "Polish"))
    assertThat(MatroskaChapterReader.readChapters(testFile).toMap()).isEqualTo(testFileChapters)
  }
}
