package de.paulwoitaschek.chapterreader.matroska

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class MatroskaChapterReaderTest {

  private val readMatroskaChapters = ReadAsMatroskaChapters()
  private val matroskaChapterReader = MatroskaChapterReader(readMatroskaChapters)

  @Test
  fun readChapters() {
    Locale.setDefault(Locale("pol", "PL", "Polish"))
    val actual = matroskaChapterReader.read(MatroskaTestFileProvider.testFile)
    assertThat(actual).isEqualTo(MatroskaTestFileProvider.testFileChapters)
  }
}
