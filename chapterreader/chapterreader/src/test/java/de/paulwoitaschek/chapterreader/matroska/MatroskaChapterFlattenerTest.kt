package de.paulwoitaschek.chapterreader.matroska

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class MatroskaChapterFlattenerTest {

  @Test
  fun test() {
    val preferredLanguages = listOf("pol", "eng")
    val actual = MatroskaChapterFlattener.toChapters(MatroskaTestFileProvider.testFileMatroskaChapters, preferredLanguages)
    assertThat(actual).isEqualTo(MatroskaTestFileProvider.testFileChapters)
  }
}
