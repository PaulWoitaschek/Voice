package de.ph1b.audiobook.features.bookOverview

import com.squareup.burst.BurstJUnit4
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests the [GridPositionResolver]
 *
 * @author Paul Woitaschek
 */
@RunWith(BurstJUnit4::class)
class GridPositionResolverTest(private val config: Config) {

  private lateinit var resolver: GridPositionResolver

  @Before
  fun setUp() {
    resolver = GridPositionResolver()
  }

  @Test
  fun test() {
    for (position in 0 until config.count) {
      resolver.prepare(position = position, count = config.count, columnCount = config.columnCount)
      val actual = config.toTest(resolver)
      val expected = config.expectedPositions.contains(position)
      assertThat(actual)
          .`as`("check $position")
          .isEqualTo(expected)
    }
  }

  enum class Config(val expectedPositions: Set<Int>, val count: Int, val columnCount: Int, val toTest: GridPositionResolver.() -> Boolean) {

    LEFT_ONE_COLUMN(expectedPositions = setOf(0, 1, 2, 3, 4), count = 5, columnCount = 1, toTest = { isLeft() }),
    LEFT_TWO_COLUMN(expectedPositions = setOf(0, 2, 4), count = 5, columnCount = 2, toTest = { isLeft() }),
    LEFT_THREE_COLUMN(expectedPositions = setOf(0, 3), count = 5, columnCount = 3, toTest = { isLeft() }),

    TOP_ONE_COLUMN(expectedPositions = setOf(0), count = 5, columnCount = 1, toTest = { isTop() }),
    TOP_TWO_COLUMN(expectedPositions = setOf(0, 1), count = 5, columnCount = 2, toTest = { isTop() }),
    TOP_THREE_COLUMN(expectedPositions = setOf(0, 1, 2), count = 5, columnCount = 3, toTest = { isTop() }),

    RIGHT_ONE_COLUMN(expectedPositions = setOf(0, 1, 2, 3, 4), count = 5, columnCount = 1, toTest = { isRight() }),
    RIGHT_TWO_COLUMN(expectedPositions = setOf(1, 3), count = 5, columnCount = 2, toTest = { isRight() }),
    RIGHT_THREE_COLUMN(expectedPositions = setOf(2), count = 5, columnCount = 3, toTest = { isRight() }),

    BOTTOM_ONE_COLUMN(expectedPositions = setOf(4), count = 5, columnCount = 1, toTest = { isBottom() }),
    BOTTOM_TWO_COLUMN_EVEN(expectedPositions = setOf(2, 3), count = 4, columnCount = 2, toTest = { isBottom() }),
    BOTTOM_TWO_COLUMN_UNEVEN(expectedPositions = setOf(4), count = 5, columnCount = 2, toTest = { isBottom() }),
    BOTTOM_THREE_COLUMN_EVEN(expectedPositions = setOf(3, 4, 5), count = 6, columnCount = 3, toTest = { isBottom() }),
    BOTTOM_THREE_COLUMN_UNEVEN(expectedPositions = setOf(3, 4), count = 5, columnCount = 3, toTest = { isBottom() }),
  }
}
