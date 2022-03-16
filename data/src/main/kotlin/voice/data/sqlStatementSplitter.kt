package voice.data

import androidx.annotation.VisibleForTesting


private const val SQLITE_MAX_VARIABLE_NUMBER = 990

// we can only query SQLITE_MAX_VARIABLE_NUMBER at once (999 bugs on some devices so we use a number a little smaller.)
// if it's larger than the limit, we query in chunks.
internal inline fun <T, R> List<T>.runForMaxSqlVariableNumber(
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  limit: Int = SQLITE_MAX_VARIABLE_NUMBER,
  action: (List<T>) -> List<R>,
): List<R> {
  return when {
    size == 0 -> emptyList()
    size <= limit -> action(this)
    else -> chunked(limit).flatMap(action)
  }
}
