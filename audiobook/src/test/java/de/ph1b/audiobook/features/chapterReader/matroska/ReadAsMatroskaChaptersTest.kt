package de.ph1b.audiobook.features.chapterReader.matroska

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class ReadAsMatroskaChaptersTest {

  @Test
  fun readMatroskaChaptersTest() {
    val actual = ReadAsMatroskaChapters.read(MatroskaTestFileProvider.testFile)
    assertThat(actual).isEqualTo(MatroskaTestFileProvider.testFileMatroskaChapters)
  }
}
