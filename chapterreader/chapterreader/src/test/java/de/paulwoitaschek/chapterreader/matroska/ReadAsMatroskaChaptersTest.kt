package de.paulwoitaschek.chapterreader.matroska

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReadAsMatroskaChaptersTest {

  private val readMatroskaChapters = ReadAsMatroskaChapters()

  @Test
  fun readMatroskaChaptersTest() {
    val actual = readMatroskaChapters.read(MatroskaTestFileProvider.testFile)
    assertThat(actual).isEqualTo(MatroskaTestFileProvider.testFileMatroskaChapters)
  }
}
