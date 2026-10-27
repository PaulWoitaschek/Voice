package voice.core.search

internal fun String.typoPrefixLength(term: String): Int? {
  if (length < term.length - 1) return null
  val columns = minOf(length, term.length + 1)
  val edits = Array(term.length + 1) { row -> IntArray(columns + 1) { column -> if (row == 0) column else row } }
  for (row in 1..term.length) {
    for (column in 1..columns) {
      val replaced = edits[row - 1][column - 1] + if (term[row - 1] == this[column - 1]) 0 else 1
      var best = minOf(replaced, edits[row - 1][column] + 1, edits[row][column - 1] + 1)
      if (row > 1 && column > 1 && term[row - 1] == this[column - 2] && term[row - 2] == this[column - 1]) {
        best = minOf(best, edits[row - 2][column - 2] + 1)
      }
      edits[row][column] = best
    }
  }
  return (columns downTo 1).firstOrNull { edits[term.length][it] <= 1 }
}
