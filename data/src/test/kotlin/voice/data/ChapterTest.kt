package voice.data

import io.kotest.matchers.collections.shouldContainInOrder
import org.junit.Test
import java.time.Instant

class ChapterTest {

  @Test
  fun `chapter parsing does best effort on broken marks`() {
    val positions = Chapter(
      duration = 20L,
      fileLastModified = Instant.now(),
      id = ChapterId(""),
      markData = listOf(11, 5, 5, 10).mapIndexed { index, i ->
        MarkData(
          startMs = i.toLong(),
          name = "Mark $index",
        )
      },
      name = "Chapter",
    ).chapterMarks.map { MarkPosition(it.startMs, it.endMs) }

    positions.shouldContainInOrder(
      MarkPosition(0L, 9L),
      MarkPosition(10L, 19L),
    )
  }

  data class MarkPosition(val start: Long, val end: Long)
}
