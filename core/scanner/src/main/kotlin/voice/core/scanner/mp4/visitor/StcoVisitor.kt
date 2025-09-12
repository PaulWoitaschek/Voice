package voice.core.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import dev.zacsweers.metro.Inject
import voice.core.logging.api.Logger
import voice.core.scanner.mp4.Mp4ChpaterExtractorOutput

// https://developer.apple.com/documentation/quicktime-file-format/chunk_offset_atom
@Inject
internal class StcoVisitor : AtomVisitor {

  override val path: List<String> = listOf("moov", "trak", "mdia", "minf", "stbl", "stco")

  override fun visit(
    buffer: ParsableByteArray,
    parseOutput: Mp4ChpaterExtractorOutput,
  ) {
    val version = buffer.readUnsignedByte()
    if (version != 0) {
      Logger.w("Unexpected version $version in stco atom, expected 0")
    } else {
      buffer.skipBytes(3) // flags
      val numberOfEntries = buffer.readUnsignedIntToInt()
      Logger.v("Number of entries in stco: $numberOfEntries")
      val chunkOffsets = (0 until numberOfEntries).map { buffer.readUnsignedInt() }
      parseOutput.chunkOffsets.add(chunkOffsets)
    }
  }
}
