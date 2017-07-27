package de.ph1b.audiobook.chapterreader.matroska


import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class MatroskaChapterFlattenerTest {

  @Test
  fun test() {
    val preferredLanguages = listOf("pol", "eng")
    val actual = MatroskaChapterFlattener.toMap(MatroskaTestFileProvider.testFileMatroskaChapters, preferredLanguages)
    assertThat(actual).isEqualTo(MatroskaTestFileProvider.testFileChapters)
  }
}
