package voice.data

import io.kotest.matchers.collections.shouldContainInOrder
import org.junit.Test
import java.time.Instant

class ChapterTest {

  @Test
  fun `filters duplicates`() {
    test(
      chapterStarts = listOf(0, 5, 5, 10),
      expected = listOf(
        MarkPosition(0, 4),
        MarkPosition(5, 9),
        MarkPosition(10, 19),
      ),
      duration = 20,
    )
  }

  @Test
  fun `missing start position`() {
    test(
      chapterStarts = listOf(5, 10),
      expected = listOf(
        MarkPosition(0, 9),
        MarkPosition(10, 19),
      ),
      duration = 20,
    )
  }

  @Test
  fun `exceeding end position`() {
    test(
      chapterStarts = listOf(0, 10, 19),
      expected = listOf(
        MarkPosition(0, 9),
        MarkPosition(10, 19),
      ),
      duration = 20,
    )
  }

  @Test
  fun `marks without duration`() {
    test(
      chapterStarts = listOf(0, 5, 6, 7, 10),
      expected = listOf(
        MarkPosition(0, 4),
        MarkPosition(5, 9),
        MarkPosition(10, 19),
      ),
    )
  }

  @Test
  fun `on single chapter creates fallback`() {
    test(
      chapterStarts = listOf(5),
      expected = listOf(
        MarkPosition(0, 19),
      ),
    )
  }

  @Test
  fun `on missing chapters creates a single fallback`() {
    test(
      chapterStarts = listOf(),
      expected = listOf(
        MarkPosition(0, 19),
      ),
    )
  }

  @Test
  fun `negative start points are ignored`() {
    test(
      chapterStarts = listOf(-2, 5),
      expected = listOf(
        MarkPosition(0, 19),
      ),
    )
  }

  @Test
  fun `negative start points without following below zero are ignored`() {
    test(
      chapterStarts = listOf(-2, 0),
      expected = listOf(
        MarkPosition(0, 19),
      ),
    )
  }

  @Test
  fun `no duration mark on first position`() {
    test(
      chapterStarts = listOf(0, 1, 5),
      expected = listOf(
        MarkPosition(0, 4),
        MarkPosition(5, 19),
      ),
      duration = 20,
    )
  }

  private fun test(
    chapterStarts: List<Int>,
    expected: List<MarkPosition>,
    duration: Long = 20L,
  ) {
    val positions = Chapter(
      duration = duration,
      fileLastModified = Instant.now(),
      id = ChapterId(""),
      markData = chapterStarts.sorted().mapIndexed { index, i ->
        MarkData(
          startMs = i.toLong(),
          name = "Mark $index",
        )
      },
      name = "Chapter",
    ).chapterMarks.map { MarkPosition(it.startMs, it.endMs) }

    positions.shouldContainInOrder(expected)
  }

  data class MarkPosition(
    val start: Long,
    val end: Long,
  )
}
