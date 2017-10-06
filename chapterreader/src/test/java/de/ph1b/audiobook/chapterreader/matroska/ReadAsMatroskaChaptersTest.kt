package de.ph1b.audiobook.chapterreader.matroska

import de.ph1b.audiobook.chapterreader.NoOpLogger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ReadAsMatroskaChaptersTest {

  private val readMatroskaChapters = ReadAsMatroskaChapters(NoOpLogger)

  @Test
  fun readMatroskaChaptersTest() {
    val actual = readMatroskaChapters.read(MatroskaTestFileProvider.testFile)
    assertThat(actual).isEqualTo(MatroskaTestFileProvider.testFileMatroskaChapters)
  }
}
