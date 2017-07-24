package de.ph1b.audiobook.features.chapterReader.matroska


import de.ph1b.audiobook.misc.toMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class MatroskaChapterFlattenerTest {

  @Test
  fun test() {
    val preferredLanguages = listOf("pol", "eng")
    val actual = MatroskaChapterFlattener.toSparseArray(MatroskaTestFileProvider.testFileMatroskaChapters, preferredLanguages)
        .toMap()
    assertThat(actual).isEqualTo(MatroskaTestFileProvider.testFileChapters)
  }
}
