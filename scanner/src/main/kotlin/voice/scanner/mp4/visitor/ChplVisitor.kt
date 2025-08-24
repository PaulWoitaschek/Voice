package voice.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import dev.zacsweers.metro.Inject
import voice.scanner.mp4.Mp4ChpaterExtractorOutput
import voice.data.MarkData
import voice.logging.core.Logger

@Inject
internal class ChplVisitor : AtomVisitor {

  override val path: List<String> = listOf("moov", "udta", "chpl")

  override fun visit(
      buffer: ParsableByteArray,
      parseOutput: Mp4ChpaterExtractorOutput,
  ) {
    buffer.setPosition(0)
    val version = buffer.readUnsignedByte()
    buffer.skipBytes(3) // flags

    if (version != 0 && version != 1) {
      Logger.w("Unexpected version $version in chpl atom, expected 0 or 1")
      return
    }

    if (version == 1) {
      buffer.skipBytes(4)
    }

    val chapterCount = buffer.readUnsignedByte()

    val chapters = (0 until chapterCount).map {
      val timestamp = if (version == 0) {
        buffer.readUnsignedInt()
      } else {
        buffer.readUnsignedLongToLong()
      }

      val titleLength = buffer.readUnsignedByte()
      val title = buffer.readString(titleLength)

      // Convert from 100ns units to milliseconds (10,000 units per ms)
      val startTimeMs = timestamp / 10_000
      MarkData(startMs = startTimeMs, name = title)
    }

    parseOutput.chplChapters = chapters
  }
}
