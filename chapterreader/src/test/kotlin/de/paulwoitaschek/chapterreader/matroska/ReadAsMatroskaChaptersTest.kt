package de.paulwoitaschek.chapterreader.matroska

import io.kotest.matchers.shouldBe
import org.junit.Test

class ReadAsMatroskaChaptersTest {

  private val readMatroskaChapters = ReadAsMatroskaChapters()

  @Test
  fun readMatroskaChaptersTest() {
    val actual = readMatroskaChapters.read(MatroskaTestFileProvider.testFile)
    actual shouldBe MatroskaTestFileProvider.testFileMatroskaChapters
  }
}
