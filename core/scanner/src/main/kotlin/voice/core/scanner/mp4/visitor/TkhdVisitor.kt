package voice.core.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import dev.zacsweers.metro.Inject
import voice.core.scanner.mp4.Mp4ChpaterExtractorOutput

@Inject
internal class TkhdVisitor : AtomVisitor {

  override val path =
    listOf("moov", "trak", "tkhd")

  override fun visit(
    buffer: ParsableByteArray,
    parseOutput: Mp4ChpaterExtractorOutput,
  ) {
    val version = buffer.readUnsignedByte()

    buffer.skipBytes(3) // flags

    val trackId =
      if (version == 1) {
        buffer.skipBytes(16)
        buffer.readInt()
      } else {
        buffer.skipBytes(8)
        buffer.readInt()
      }

    parseOutput.trackIds.add(trackId)
  }
}
