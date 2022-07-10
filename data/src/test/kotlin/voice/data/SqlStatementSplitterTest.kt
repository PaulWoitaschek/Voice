package voice.data

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Test

class SqlStatementSplitterTest {

  @Test
  fun chunked() {
    val numbers = listOf(1, 2, 3)
    val recordedChunks = mutableListOf<List<Int>>()
    val result = numbers.runForMaxSqlVariableNumber(2) { chunk ->
      recordedChunks += chunk
      chunk.map { it.toString() }
    }
    recordedChunks.shouldContainExactly(
      listOf(
        listOf(1, 2),
        listOf(3)
      )
    )
    result.shouldContainExactly("1", "2", "3")
  }

  @Test
  fun lessThanThreshold() {
    val numbers = listOf(1, 2, 3)
    val recordedChunks = mutableListOf<List<Int>>()
    val result = numbers.runForMaxSqlVariableNumber(4) { chunk ->
      recordedChunks += chunk
      chunk.map { it.toString() }
    }
    recordedChunks.shouldContainExactly(
      listOf(
        listOf(1, 2, 3)
      )
    )
    result.shouldContainExactly("1", "2", "3")
  }

  @Test
  fun noElements() {
    val numbers = emptyList<Int>()
    val recordedChunks = mutableListOf<List<Int>>()
    val result = numbers.runForMaxSqlVariableNumber<Int, String>(4) {
      error("Should not be called")
    }
    recordedChunks.shouldBeEmpty()
    result.shouldBeEmpty()
  }
}
