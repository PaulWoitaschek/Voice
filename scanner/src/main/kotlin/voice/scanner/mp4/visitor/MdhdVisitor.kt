package voice.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import dev.zacsweers.metro.Inject
import voice.scanner.mp4.Mp4ChpaterExtractorOutput
import voice.logging.core.Logger

// https://developer.apple.com/documentation/quicktime-file-format/media_header_atom
@Inject
internal class MdhdVisitor : AtomVisitor {

  override val path: List<String> = listOf("moov", "trak", "mdia", "mdhd")

  override fun visit(
      buffer: ParsableByteArray,
      parseOutput: Mp4ChpaterExtractorOutput,
  ) {
    val version = buffer.readUnsignedByte()
    if (version != 0 && version != 1) {
      Logger.w("Unexpected version $version in mdhd atom, expected 0 or 1")
    } else {
      val flagsSize = 3
      val creationTimeSize = if (version == 0) 4 else 8
      val modificationTimeSize = if (version == 0) 4 else 8
      buffer.skipBytes(flagsSize + creationTimeSize + modificationTimeSize)
      val timescale = buffer.readUnsignedInt()
      Logger.v("Timescale: $timescale")
      parseOutput.timeScales += timescale
    }
  }
}
