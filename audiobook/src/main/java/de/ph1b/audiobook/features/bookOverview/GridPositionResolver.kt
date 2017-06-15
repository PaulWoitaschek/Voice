package de.ph1b.audiobook.features.bookOverview

/**
 * Resolves which side a position is in a grid.
 *
 * @author Paul Woitaschek
 */
class GridPositionResolver {

  private var position = 0
  private var count = 0
  private var columnCount = 1

  fun prepare(position: Int, count: Int, columnCount: Int) {
    require(columnCount > 0) { "columnCount $columnCount must be positive" }
    require(position >= 0) { "position $position must not be negative" }
    require(count >= 0) { "count $count must not be negative" }
    require(position < count) { "position=$position must be < count=$count" }
    this.position = position
    this.count = count
    this.columnCount = columnCount
  }

  fun isLeft() = position % columnCount == 0

  fun isTop() = position < columnCount

  fun isRight() = (position + 1) % columnCount == 0

  fun isBottom(): Boolean {
    val row = position / columnCount
    val lastRow = (count - 1) / columnCount
    return row == lastRow
  }
}
