package de.ph1b.audiobook.features.chapterReader.matroska

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class ReadAsMatroskaChaptersTest {

  @Test
  fun readMatroskaChaptersTest() {
    val chapters = ReadAsMatroskaChapters.read(MatroskaTestFileProvider.testFile)
    Assertions.assertThat(chapters).isEqualTo(MatroskaTestFileProvider.testFileMatroskaChapters)
  }
}
