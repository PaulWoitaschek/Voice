package de.paulwoitaschek.chapterreader.matroska

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class MatroskaChapterTest {

  @Test
  fun getName() {
    val chapter = MatroskaChapter(
      0L,
      listOf(
        MatroskaChapterName("Podczęść 1", setOf("pol")),
        MatroskaChapterName("Subpart 1", setOf("eng", "ger")),
        MatroskaChapterName("サブパート1", setOf("jpn")),
      ),
      listOf(),
    )
    chapter.name() shouldBe "Podczęść 1"
    chapter.name(listOf("ger", "jpn")) shouldBe "Subpart 1"
    chapter.name(listOf("ind", "kac", "jpn", "eng")) shouldBe "サブパート1"
  }

  @Test
  fun noContentsLeadsToNull() {
    val actual = MatroskaChapter(0L, listOf(), listOf()).name()
    actual.shouldBeNull()
  }
}
