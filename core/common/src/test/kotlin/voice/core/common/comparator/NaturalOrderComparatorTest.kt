package voice.core.common.comparator

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NaturalOrderComparatorTest {

  private fun assertSorted(values: List<String>) {
    assertEquals(
      expected = values,
      actual =
        values
          .toMutableList()
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

  @Test
  fun `letters and symbols sort in one order`() {
    assertSorted(
      listOf(
        "~",
        "ä",
        "b",
      ),
    )
  }

  @Test
  fun `the order has no circles`() {
    val comparator = NaturalOrderComparator.stringComparator
    val random = Random(42)
    val chars = "aAbäÄ~_-. 019２²٠éz!"
    val values =
      List(60) {
        String(CharArray(random.nextInt(1, 4)) { chars[random.nextInt(chars.length)] })
      }
    for (a in values) {
      for (b in values) {
        for (c in values) {
          if (comparator.compare(a, b) <= 0 && comparator.compare(b, c) <= 0) {
            assertTrue(comparator.compare(a, c) <= 0)
          }
        }
      }
    }
  }
}
