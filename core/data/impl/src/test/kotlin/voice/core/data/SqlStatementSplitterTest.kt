package voice.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlStatementSplitterTest {

  @Test
  fun chunked() {
    val numbers = listOf(1, 2, 3)
    val recordedChunks = mutableListOf<List<Int>>()
    val result = numbers.runForMaxSqlVariableNumber(2) { chunk ->
      recordedChunks += chunk
      chunk.map { it.toString() }
    }
    assertEquals(
      expected = listOf(
        listOf(1, 2),
        listOf(3),
      ),
      actual = recordedChunks,
    )
    assertEquals(expected = listOf("1", "2", "3"), actual = result)
  }

  @Test
  fun lessThanThreshold() {
    val numbers = listOf(1, 2, 3)
    val recordedChunks = mutableListOf<List<Int>>()
    val result = numbers.runForMaxSqlVariableNumber(4) { chunk ->
      recordedChunks += chunk
      chunk.map { it.toString() }
    }
    assertEquals(
      expected = listOf(
        listOf(1, 2, 3),
      ),
      actual = recordedChunks,
    )
    assertEquals(expected = listOf("1", "2", "3"), actual = result)
  }

  @Test
  fun noElements() {
    val numbers = emptyList<Int>()
    val recordedChunks = mutableListOf<List<Int>>()
    val result = numbers.runForMaxSqlVariableNumber<Int, String>(4) {
      error("Should not be called")
    }
    assertTrue(recordedChunks.isEmpty())
    assertTrue(result.isEmpty())
  }
}
