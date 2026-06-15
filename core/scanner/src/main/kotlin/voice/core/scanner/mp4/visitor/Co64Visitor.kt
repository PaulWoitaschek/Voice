package voice.core.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import dev.zacsweers.metro.Inject
import voice.core.logging.api.Logger
import voice.core.scanner.mp4.Mp4ChpaterExtractorOutput

@Inject
internal class Co64Visitor : AtomVisitor {

  override val path =
    listOf("moov", "trak", "mdia", "minf", "stbl", "co64")

  override fun visit(
    buffer: ParsableByteArray,
    parseOutput: Mp4ChpaterExtractorOutput,
  ) {
    val version = buffer.readUnsignedByte()

    if (version != 0) {
      Logger.w("Unexpected version $version in co64 atom")
      return
    }

    buffer.skipBytes(3) // flags

    val numberOfEntries = buffer.readUnsignedIntToInt()

    val chunkOffsets =
      (0 until numberOfEntries).map {
        buffer.readUnsignedLongToLong()
      }

    parseOutput.chunkOffsets.add(chunkOffsets)
  }
}
