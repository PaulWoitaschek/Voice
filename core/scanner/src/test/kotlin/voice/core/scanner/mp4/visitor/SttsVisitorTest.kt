package voice.core.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import voice.core.scanner.mp4.Mp4ChpaterExtractorOutput
import voice.core.scanner.mp4.SttsEntry
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SttsVisitorTest {

  @Test
  fun `keeps sample durations compact`() {
    val output = Mp4ChpaterExtractorOutput()

    SttsVisitor().visit(
      buffer = sttsBuffer(
        sampleCount = 3_000_000_000,
        sampleDuration = 1024,
      ),
      parseOutput = output,
    )

    assertEquals(
      expected = listOf(
        SttsEntry(
          sampleCount = 3_000_000_000,
          sampleDuration = 1024,
        ),
      ),
      actual = output.durations.single(),
    )
  }

  private fun sttsBuffer(
    sampleCount: Long,
    sampleDuration: Long,
  ): ParsableByteArray {
    val bytes = ByteBuffer.allocate(16)
      .put(0)
      .put(byteArrayOf(0, 0, 0))
      .putInt(1)
      .putInt(sampleCount.toInt())
      .putInt(sampleDuration.toInt())
      .array()
    return ParsableByteArray(bytes)
  }
}
