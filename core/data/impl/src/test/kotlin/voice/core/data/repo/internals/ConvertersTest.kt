package voice.core.data.repo.internals

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.toKotlinInstant
import kotlin.uuid.Uuid
import voice.core.data.MarkData

class ConvertersTest {

  @Test
  fun instant() {
    test(Clock.System.now(), Converters::fromInstant, Converters::toInstant)
  }

  @Test
  fun `instants stored by java time stay the same`() {
    val converters = Converters()
    listOf(
        "1970-01-01T00:00:00Z",
        "2026-10-07T10:00:00.123Z",
        "2026-10-07T10:00:00.123456Z",
        "2026-10-07T10:00:00.123456789Z",
        "+12000-01-01T00:00:00Z",
      )
      .forEach { stored ->
        val instant = converters.toInstant(stored)
        assertEquals(java.time.Instant.parse(stored).toKotlinInstant(), instant)
        assertEquals(stored, converters.fromInstant(instant))
      }
  }

  @Test
  fun uuid() {
    test(Uuid.random(), Converters::fromUuid, Converters::toUuid)
  }

  @Test
  fun file() {
    test(File("/sdcard/audiobooks/potter.mp3"), Converters::fromFile, Converters::toFile)
  }

  @Test
  fun marksEmpty() {
    test(emptyList(), Converters::fromMarks, Converters::toMarks)
  }

  @Test
  fun marks() {
    test(
      value =
        listOf(
          MarkData(0L, "Hello"),
          MarkData(5L, "World"),
          MarkData(Long.MIN_VALUE, ""),
        ),
      serialize = Converters::fromMarks,
      deSerialize = Converters::toMarks,
    )
  }

  private fun <S, D> test(
    value: D,
    serialize: (Converters.(D) -> S),
    deSerialize: (Converters.(S) -> D),
  ) {
    val converters = Converters()
    val serialized: S = converters.serialize(value)
    val deSerialized: D = converters.deSerialize(serialized)
    assertEquals(expected = value, actual = deSerialized)
  }
}
