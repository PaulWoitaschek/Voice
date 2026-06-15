package voice.core.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import dev.zacsweers.metro.Inject
import voice.core.data.MarkData
import voice.core.logging.api.Logger
import voice.core.scanner.mp4.Mp4ChpaterExtractorOutput

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
    Logger.e("CHPL DEBUG version=$version chapterCount=$chapterCount")

    val chapters = (0 until chapterCount).map { index ->
      val timestamp = if (version == 0) {
        buffer.readUnsignedInt()
      } else {
        buffer.readUnsignedLongToLong()
      }

      val titleLength = buffer.readUnsignedByte()
      val title = if (titleLength > 0) {
        val titleBytes = ByteArray(titleLength)
        buffer.readBytes(titleBytes, 0, titleLength)
        String(titleBytes, Charsets.UTF_8)
      } else {
        ""
      }

      val padding = (4 - ((1 + titleLength) % 4)) % 4
      if (padding > 0) {
        buffer.skipBytes(padding)
      }

      val startTimeMs = timestamp / 10_000
      if (index < 12) {
        Logger.e("CHPL ENTRY $index ts=$timestamp startTimeMs=$startTimeMs titleLength=$titleLength title=$title padding=$padding")
      }

      MarkData(startMs = startTimeMs, name = title)
    }

    parseOutput.chplChapters = chapters
  }
}
