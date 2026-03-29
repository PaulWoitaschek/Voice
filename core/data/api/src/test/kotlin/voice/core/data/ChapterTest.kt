package voice.core.data

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainInOrder
import org.junit.Test
import java.time.Instant

class ChapterTest {

  @Test
  fun `duplicates are ignored`() = test2(
    """
    A#...................
    B.....#..............
    C.....#..............
    D..........#.........
    =AAAAABBBBBCCCCCCCCCC
    """,
  )

  @Test
  fun `missing start at 0 creates implicit leading mark`() = test2(
    """
    A.....#..............
    B..........#.........
    =AAAAAAAAAABBBBBBBBBB
    """,
  )

  @Test
  fun `adjacent starts are ignored`() = test2(
    """
    A#...................
    B.....#..............
    C......#.............
    D.......#............
    E..........#.........
    =AAAAABBBBBCCCCCCCCCC
    """,
  )

  @Test
  fun `start at last tick does not create a new segment`() = test2(
    """
    A#...................
    B..........#.........
    C...................#
    =AAAAAAAAAABBBBBBBBBB
    """,
  )

  @Test
  fun `no valid starts creates single fallback`() = test2(
    """
    =AAAAAAAAAAAAAAAAAAAA
    """,
  )

  @Test
  fun `negative-only starts create single fallback`() = test2(
    """
    A@-2
    B@-1
    =AAAAAAAAAAAAAAAAAAAA
    """,
  )

  @Test
  fun `single nonzero start creates single fallback`() = test2(
    """
    A.....#..............
    =AAAAAAAAAAAAAAAAAAAA
    """,
  )

  @Test
  fun `first mark at tick 1 is ignored`() = test2(
    """
    A#...................
    B.#..................
    C.....#..............
    =AAAAABBBBBBBBBBBBBBB
    """,
  )

  /**
   * Visual DSL:
   *
   * Start lines:
   * - Timeline form: `A#.....` where first char is label and `#` marks the start tick.
   * - Numeric form: `A@-2` for explicit (esp. negative) starts.
   *
   * Expected line (must be last):
   * - `=AAAABBB...` (each char = one tick; length = duration)
   */
  private fun test2(case: String) {
    val lines = case
      .trimIndent()
      .lineSequence()
      .map { it.trimEnd() }
      .filter { it.isNotBlank() }
      .toList()

    check(lines.isNotEmpty()) { "Need at least an expected line (e.g. =AAAA)" }

    val expectedLine = lines.last().trim()
    check(expectedLine.startsWith("=")) { "Expected line must start with '=' (e.g. =AAABBB)" }

    val expectedCoverage = expectedLine.drop(1)
    val timelineLen = expectedCoverage.length
    check(timelineLen > 0) { "Expected coverage must not be empty" }

    val startLines = lines.dropLast(1)

    // Enforce visual alignment: starts and expected must share the same timeline length.
    startLines.forEach { line ->
      when {
        '@' in line -> Unit // numeric form
        else -> {
          check(line.length == timelineLen + 1) {
            "Start line must be 1(label) + $timelineLen(timeline) chars, got ${line.length}: `$line`"
          }
          check(line.count { it == '#' } == 1) {
            "Each timeline start line must contain exactly one '#': `$line`"
          }
        }
      }
    }

    val marks = startLines.mapNotNull { line ->
      when {
        '@' in line -> {
          val start = line.substringAfter('@').trim().toLongOrNull()
            ?: error("Bad numeric start line: `$line` (expected like A@-2)")
          MarkData(startMs = start, name = line.first().toString())
        }
        '#' in line -> {
          val startTick = line.indexOf('#') - 1
          MarkData(startMs = startTick.toLong(), name = line.first().toString())
        }
        else -> null
      }
    }

    val duration = timelineLen.toLong()

    val actualPositions = Chapter(
      duration = duration,
      fileLastModified = Instant.now(),
      id = ChapterId(""),
      markData = marks,
      name = "Chapter",
    ).chapterMarks.map { MarkPosition(it.startMs, it.endMs) }

    val expectedPositions = coverageToPositions(expectedCoverage)
    val actualCoverage = positionsToCoverage(actualPositions, timelineLen)

    withClue(
      """
      Expected: $expectedCoverage
      Actual  : $actualCoverage
      Starts  : ${marks.map { it.startMs }.sorted()}
      Duration: $duration
      """.trimIndent(),
    ) {
      actualPositions.shouldContainInOrder(expectedPositions)
    }
  }

  private fun coverageToPositions(coverage: String): List<MarkPosition> {
    if (coverage.isEmpty()) return emptyList()

    val out = mutableListOf<MarkPosition>()
    var start = 0
    var current = coverage[0]

    for (i in 1 until coverage.length) {
      val c = coverage[i]
      if (c != current) {
        out += MarkPosition(start.toLong(), (i - 1).toLong())
        start = i
        current = c
      }
    }
    out += MarkPosition(start.toLong(), (coverage.length - 1).toLong())
    return out
  }

  private fun positionsToCoverage(
    positions: List<MarkPosition>,
    length: Int,
  ): String {
    if (length <= 0) return ""
    val chars = CharArray(length) { '.' }

    for ((idx, p) in positions.withIndex()) {
      val label = ('A'.code + idx).toChar()
      val s = p.start.toInt().coerceAtLeast(0)
      val e = p.end.toInt().coerceAtMost(length - 1)
      for (i in s..e) chars[i] = label
    }

    return String(chars)
  }

  data class MarkPosition(
    val start: Long,
    val end: Long,
  )
}
