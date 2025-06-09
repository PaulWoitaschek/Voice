package voice.app.scanner

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import voice.app.scanner.matroska.MatroskaChapter
import voice.app.scanner.matroska.MatroskaChapterName

class MatroskaChapterTest {

  @Test
  fun getName() {
    val chapter = MatroskaChapter(
      startTime = 0L,
      names = listOf(
        MatroskaChapterName("Podczęść 1", setOf("pol")),
        MatroskaChapterName("Subpart 1", setOf("eng", "ger")),
        MatroskaChapterName("サブパート1", setOf("jpn")),
      ),
    )
    chapter.bestName(emptyList()) shouldBe "Podczęść 1"
    chapter.bestName(listOf("ger", "jpn")) shouldBe "Subpart 1"
    chapter.bestName(listOf("ind", "kac", "jpn", "eng")) shouldBe "サブパート1"
  }

  @Test
  fun noContentsLeadsToNull() {
    val actual = MatroskaChapter(0L, listOf()).bestName(emptyList())
    actual.shouldBeNull()
  }
}
