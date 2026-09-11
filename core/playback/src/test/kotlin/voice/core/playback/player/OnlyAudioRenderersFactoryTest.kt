package voice.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyAudioRenderersFactoryTest {

  @Test
  fun `long low-volume audio at start and end is shortened`() {
    val lowVolumeFrames = ShortArray(4_000) { LOW_VOLUME_SAMPLE }
    val spokenFrames = ShortArray(1_000) { SPOKEN_AUDIO_SAMPLE }

    val leadingLowVolumeOutput = processFrames(lowVolumeFrames + spokenFrames)
    val trailingLowVolumeOutput = processFrames(spokenFrames + lowVolumeFrames)

    assertEquals(1_500, leadingLowVolumeOutput)
    assertEquals(1_500, trailingLowVolumeOutput)
  }

  @Test
  fun `short low-volume audio is preserved`() {
    val input = ShortArray(500) { LOW_VOLUME_SAMPLE }

    val outputFrameCount = processFrames(input)

    assertEquals(input.size, outputFrameCount)
  }

  @Suppress("DEPRECATION")
  private fun processFrames(samples: ShortArray): Int {
    val processor = audiobookSilenceSkippingAudioProcessor()
    processor.setEnabled(true)
    processor.configure(AudioProcessor.AudioFormat(SAMPLE_RATE, 1, C.ENCODING_PCM_16BIT))
    processor.flush()

    val input = ByteBuffer.allocateDirect(samples.size * Short.SIZE_BYTES)
      .order(ByteOrder.nativeOrder())
    samples.forEach { sample ->
      input.putShort(sample)
    }
    input.flip()

    var outputByteCount = 0
    while (input.hasRemaining()) {
      processor.queueInput(input)
      outputByteCount += processor.output.remaining()
    }
    processor.queueEndOfStream()
    while (!processor.isEnded) {
      outputByteCount += processor.output.remaining()
    }

    return outputByteCount / Short.SIZE_BYTES
  }
}

private const val SAMPLE_RATE = 1_000
private const val LOW_VOLUME_SAMPLE: Short = 1_500
private const val SPOKEN_AUDIO_SAMPLE: Short = 5_000
