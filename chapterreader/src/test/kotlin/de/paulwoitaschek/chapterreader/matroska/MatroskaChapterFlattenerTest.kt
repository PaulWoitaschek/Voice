package de.paulwoitaschek.chapterreader.matroska

import io.kotest.matchers.shouldBe
import org.junit.Test

class MatroskaChapterFlattenerTest {

  @Test
  fun test() {
    val preferredLanguages = listOf("pol", "eng")
    val actual =
      MatroskaChapterFlattener.toChapters(MatroskaTestFileProvider.testFileMatroskaChapters, preferredLanguages)
    actual shouldBe MatroskaTestFileProvider.testFileChapters
  }
}
