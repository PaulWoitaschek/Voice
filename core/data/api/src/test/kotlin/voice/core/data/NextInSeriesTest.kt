package voice.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NextInSeriesTest {

  @Test
  fun `book 3 completed before, book 4 not started, book 2 ends, up next is book 4`() {
    val second = completed(part = "2")
    val third = completed(part = "3")
    val fourth = notStarted(part = "4")

    assertEquals(expected = fourth, actual = listOf(fourth, third, second).nextInSeries(second))
  }

  @Test
  fun `book 3 hidden, book 4 not started, book 2 ends, up next is book 4`() {
    val second = completed(part = "2")
    val third = notStarted(part = "3", hidden = true)
    val fourth = notStarted(part = "4")

    assertEquals(expected = fourth, actual = listOf(second, third, fourth).nextInSeries(second))
  }

  @Test
  fun `the last book of a series ends, no up next`() {
    val first = completed(part = "1")
    val second = completed(part = "2")

    assertNull(listOf(first, second).nextInSeries(second))
  }

  @Test
  fun `book 1 not completed, book 3 of 3 ends, no up next`() {
    val first = notStarted(part = "1")
    val second = completed(part = "2")
    val third = completed(part = "3")

    assertNull(listOf(first, second, third).nextInSeries(third))
  }

  @Test
  fun `a book without a series ends, no up next`() {
    val book = book(name = "A", time = 20_000)
    val next = book(name = "B", time = 0)

    assertNull(listOf(book, next, notStarted(part = "1")).nextInSeries(book))
  }

  @Test
  fun `the next book follows the order of the series screen and its name however it's spelled`() {
    val second = completed(part = "2")
    val tenth = notStarted(part = "10")
    val third = notStarted(part = "3", series = "SÁGA")
    val other = notStarted(part = "2.5", series = "Other")

    assertEquals(
      expected = third,
      actual = listOf(tenth, other, third, second).nextInSeries(second),
    )
  }

  private fun completed(part: String) = book(series = "Saga", part = part, time = 20_000)

  private fun notStarted(
    part: String,
    series: String = "Saga",
    hidden: Boolean = false,
  ) = book(series = series, part = part, time = 0, hidden = hidden)
}
