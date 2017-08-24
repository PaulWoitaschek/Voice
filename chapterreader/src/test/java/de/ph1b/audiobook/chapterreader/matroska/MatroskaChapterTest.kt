package de.ph1b.audiobook.chapterreader.matroska

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MatroskaChapterTest {

  @Test
  fun testGetName() {
    val chapter = MatroskaChapter(
        0L, listOf(
        MatroskaChapterName("Podczęść 1", setOf("pol")),
        MatroskaChapterName("Subpart 1", setOf("eng", "ger")),
        MatroskaChapterName("サブパート1", setOf("jpn"))
    ), listOf()
    )
    assertThat(chapter.getName()).isEqualTo("Podczęść 1")
    assertThat(chapter.getName(listOf("ger", "jpn"))).isEqualTo("Subpart 1")
    assertThat(chapter.getName(listOf("ind", "kac", "jpn", "eng"))).isEqualTo("サブパート1")
  }

  @Test
  fun testNoContentsLeadsToNull() {
    val actual = MatroskaChapter(0L, listOf(), listOf()).getName()
    assertThat(actual).isNull()
  }
}
