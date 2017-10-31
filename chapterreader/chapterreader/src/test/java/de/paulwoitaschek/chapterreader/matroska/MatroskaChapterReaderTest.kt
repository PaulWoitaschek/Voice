package de.paulwoitaschek.chapterreader.matroska

import com.google.common.truth.Truth.assertThat
import de.paulwoitaschek.chapterreader.NoOpLogger
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
