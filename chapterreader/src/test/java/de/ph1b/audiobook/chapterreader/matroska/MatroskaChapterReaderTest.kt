package de.ph1b.audiobook.chapterreader.matroska

import de.ph1b.audiobook.chapterreader.NoOpLogger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*

class MatroskaChapterReaderTest {

  private val readMatroskaChapters = ReadAsMatroskaChapters(NoOpLogger)
  private val matroskaChapterReader = MatroskaChapterReader(NoOpLogger, readMatroskaChapters)

  @Test
  fun readChapters() {
    Locale.setDefault(Locale("pol", "PL", "Polish"))
    val actual = matroskaChapterReader.read(MatroskaTestFileProvider.testFile)
    assertThat(actual).isEqualTo(MatroskaTestFileProvider.testFileChapters)
  }
}
