package de.ph1b.audiobook.features.chapterReader.matroska

import de.ph1b.audiobook.misc.toMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class MatroskaChapterReaderTest {

  @Test
  fun readChapters() {
    Locale.setDefault(Locale("pol", "PL", "Polish"))
    val actual = MatroskaChapterReader.read(MatroskaTestFileProvider.testFile).toMap()
    assertThat(actual).isEqualTo(MatroskaTestFileProvider.testFileChapters)
  }
}
