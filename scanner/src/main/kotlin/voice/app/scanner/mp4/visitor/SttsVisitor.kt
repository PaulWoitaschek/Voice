package voice.app.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import voice.app.scanner.mp4.Mp4ChpaterExtractorOutput
import voice.logging.core.Logger
import javax.inject.Inject

// https://developer.apple.com/documentation/quicktime-file-format/time-to-sample_atom
class SttsVisitor @Inject constructor() : AtomVisitor {

  override val path: List<String> = listOf("moov", "trak", "mdia", "minf", "stbl", "stts")

  override fun visit(
    buffer: ParsableByteArray,
    parseOutput: Mp4ChpaterExtractorOutput,
  ) {
    val version = buffer.readUnsignedByte()
    if (version != 0) {
      Logger.w("Unexpected version $version in stts atom, expected 0")
    } else {
      buffer.skipBytes(3) // flags
      val numberOfEntriesInSttsTable = buffer.readUnsignedIntToInt()
      Logger.v("Number of entries in stts: $numberOfEntriesInSttsTable")
      val individualSampleDurations = mutableListOf<Long>()
      repeat(numberOfEntriesInSttsTable) {
        val count = buffer.readUnsignedInt().toInt()
        val delta = buffer.readUnsignedInt()
        repeat(count) {
          individualSampleDurations.add(delta)
        }
      }
      parseOutput.durations.add(individualSampleDurations)
    }
  }
}
