package voice.app.scanner.mp4

import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.text.CueDecoder
import voice.data.MarkData
import java.io.ByteArrayOutputStream

internal class ChapterCueTrackOutput(private val outputCuesList: MutableList<MarkData>) : TrackOutput {

  private val sampleDataBuffer = ByteArrayOutputStream()

  override fun format(format: Format) {}

  override fun sampleData(
    input: DataReader,
    length: Int,
    allowEndOfInput: Boolean,
    sampleDataPart: Int,
  ): Int {
    if (length == 0) return 0
    val buffer = ByteArray(length)
    val bytesRead = input.read(buffer, 0, length)
    if (bytesRead > 0) {
      sampleDataBuffer.write(buffer, 0, bytesRead)
    }
    return bytesRead
  }

  override fun sampleData(
    data: ParsableByteArray,
    length: Int,
    sampleDataPart: Int,
  ) {
    if (length == 0) return
    val currentPosition = data.position
    sampleDataBuffer.write(data.data, currentPosition, length)
    data.skipBytes(length)
  }

  override fun sampleMetadata(
    timeUs: Long,
    flags: Int,
    size: Int,
    offset: Int,
    cryptoData: TrackOutput.CryptoData?,
  ) {
    val allSampleBytes = sampleDataBuffer.toByteArray()
    val sampleEffectiveOffset = allSampleBytes.size - offset - size

    CueDecoder().decode(timeUs, allSampleBytes, sampleEffectiveOffset, size)
      .also { cuesWithTiming ->
        val cue = cuesWithTiming.cues.firstOrNull()
        if (cue != null) {
          outputCuesList += MarkData(timeUs / 1000, name = cue.text?.toString() ?: "")
        }
      }
    sampleDataBuffer.reset()
  }
}
