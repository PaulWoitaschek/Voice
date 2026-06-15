package voice.core.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import dev.zacsweers.metro.Inject
import voice.core.logging.api.Logger
import voice.core.scanner.mp4.Mp4ChpaterExtractorOutput

@Inject
internal class StszVisitor : AtomVisitor {

  override val path =
    listOf("moov", "trak", "mdia", "minf", "stbl", "stsz")

  override fun visit(
    buffer: ParsableByteArray,
    parseOutput: Mp4ChpaterExtractorOutput,
  ) {
    val version = buffer.readUnsignedByte()

    if (version != 0) {
      Logger.w("Unexpected version $version in stsz atom")
      return
    }

    buffer.skipBytes(3) // flags

    val sampleSize = buffer.readUnsignedIntToInt()
    val sampleCount = buffer.readUnsignedIntToInt()
    val sampleSizes =
      if (sampleSize != 0) {
        List(sampleCount) { sampleSize }
      } else {
        List(sampleCount) {
          buffer.readUnsignedIntToInt()
        }
      }

    parseOutput.sampleSizes.add(sampleSizes)
  }
}
