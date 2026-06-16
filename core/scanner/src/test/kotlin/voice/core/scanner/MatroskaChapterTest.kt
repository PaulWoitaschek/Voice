package voice.core.scanner

import voice.core.scanner.matroska.MatroskaChapter
import voice.core.scanner.matroska.MatroskaChapterName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    assertEquals(expected = "Podczęść 1", actual = chapter.bestName(emptyList()))
    assertEquals(expected = "Subpart 1", actual = chapter.bestName(listOf("ger", "jpn")))
    assertEquals(expected = "サブパート1", actual = chapter.bestName(listOf("ind", "kac", "jpn", "eng")))
  }

  @Test
  fun noContentsLeadsToNull() {
    val actual = MatroskaChapter(0L, listOf()).bestName(emptyList())
    assertNull(actual)
  }
}
