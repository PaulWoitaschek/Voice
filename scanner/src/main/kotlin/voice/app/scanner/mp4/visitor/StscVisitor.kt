package voice.app.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import dev.zacsweers.metro.Inject
import voice.app.scanner.mp4.Mp4ChpaterExtractorOutput
import voice.app.scanner.mp4.StscEntry
import voice.logging.core.Logger

// https://developer.apple.com/documentation/quicktime-file-format/sample-to-chunk_atom/
@Inject
class StscVisitor : AtomVisitor {

  override val path: List<String> = listOf("moov", "trak", "mdia", "minf", "stbl", "stsc")

  override fun visit(
    buffer: ParsableByteArray,
    parseOutput: Mp4ChpaterExtractorOutput,
  ) {
    val version = buffer.readUnsignedByte()
    if (version != 0) {
      Logger.w("Unexpected version $version in stsc atom, expected 0")
    } else {
      buffer.skipBytes(3) // flags
      val numberOfEntries = buffer.readUnsignedIntToInt()
      Logger.v("Number of entries in stsc: $numberOfEntries")
      val stscEntriesForTrack = (0 until numberOfEntries).map {
        val firstChunk = buffer.readUnsignedInt()
        val samplesPerChunk = buffer.readUnsignedIntToInt()
        buffer.skipBytes(4) // skip sample description index
        StscEntry(
          firstChunk = firstChunk,
          samplesPerChunk = samplesPerChunk,
        )
      }
      parseOutput.stscEntries.add(stscEntriesForTrack)
    }
  }
}
