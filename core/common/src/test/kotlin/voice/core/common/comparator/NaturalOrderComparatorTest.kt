package voice.core.common.comparator

import kotlin.test.Test
import kotlin.test.assertEquals

class NaturalOrderComparatorTest {

  private fun assertSorted(values: List<String>) {
    assertEquals(
      expected = values,
      actual = values.toMutableList()
        .apply {
          repeat(values.size / 2) {
            add(removeAt(0))
          }
        }
        .sortedWith(NaturalOrderComparator.stringComparator),
    )
  }

  @Test
  fun naturalNumberOrder() {
    assertSorted(
      listOf(
        "00 I",
        "00 Introduction",
        "1",
        "01 How to build a universe",
        "01 I",
        "2",
        "9",
        "10",
      ),
    )
  }

  @Test
  fun caseInsensitiveLetterOrder() {
    assertSorted(
      listOf(
        "a",
        "Ab",
        "aC",
        "Ba",
        "cA",
        "D",
        "e",
      ),
    )
  }

  @Test
  fun naturalFolderOrder() {
    assertSorted(
      listOf(
        "folder1/1.mp3",
        "folder1/10.mp3",
        "folder2/2.mp3",
        "folder10/1.mp3",
      ),
    )
  }

  @Test
  fun accentedLettersUseRootCollation() {
    assertSorted(
      listOf(
        "ä",
        "ä",
        "b",
        "ö",
      ),
    )
  }

  @Test
  fun accentedTitleOrder() {
    assertSorted(
      listOf(
        "Čtyři dohody",
        "Čtyři tisíce týdnů",
        "Maryša",
        "R.U.R.",
        "Zákony lidské přirozenosti",
        "Život s vysokou inteligencí",
      ),
    )
  }
}
