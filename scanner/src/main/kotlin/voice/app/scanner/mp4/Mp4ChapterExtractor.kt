package voice.app.scanner.mp4

import androidx.media3.common.util.ParsableByteArray
import androidx.media3.container.Mp4Box
import androidx.media3.extractor.ExtractorInput
import voice.data.MarkData
import voice.logging.core.Logger
import java.io.IOException

class Mp4ChapterExtractor {

  private val scratch = ParsableByteArray(Mp4Box.LONG_HEADER_SIZE)

  fun parse(input: ExtractorInput): List<MarkData> {
    return try {
      parseBoxes(input, 0, input.length)
    } catch (e: IOException) {
      Logger.w(e, "Failed to parse mp4 chapters")
      emptyList()
    } catch (e: IllegalStateException) {
      Logger.w(e, "Failed to parse mp4 chapters")
      emptyList()
    }
  }

  private fun parseBoxes(
    input: ExtractorInput,
    depth: Int,
    parentEnd: Long,
  ): List<MarkData> {
    while (input.position < parentEnd) {
      // 1) read atom header
      scratch.reset(Mp4Box.HEADER_SIZE)
      if (!input.readFully(scratch.data, 0, Mp4Box.HEADER_SIZE, true)) {
        return emptyList()
      }
      scratch.setPosition(0)
      var atomSize = scratch.readUnsignedInt()
      val atomType = scratch.readString(4)
      var headerSize = Mp4Box.HEADER_SIZE

      // 64‐bit extended size?
      if (atomSize == 1L) {
        input.readFully(
          scratch.data,
          Mp4Box.HEADER_SIZE,
          Mp4Box.LONG_HEADER_SIZE - Mp4Box.HEADER_SIZE,
        )
        scratch.setPosition(Mp4Box.HEADER_SIZE)
        atomSize = scratch.readUnsignedLongToLong()
        headerSize = Mp4Box.LONG_HEADER_SIZE
      }

      val payloadSize = (atomSize - headerSize).toInt()
      val payloadEnd = input.position + payloadSize

      when {
        depth == 0 && atomType == "moov" -> {
          val result = parseBoxes(input, depth + 1, payloadEnd)
          if (result.isNotEmpty()) return result
        }

        depth == 1 && atomType == "udta" -> {
          val result = parseBoxes(input, depth + 1, payloadEnd)
          if (result.isNotEmpty()) return result
        }
        depth == 2 && atomType == "chpl" -> {
          val buf = ParsableByteArray(payloadSize)
          if (!input.readFully(buf.data, 0, payloadSize, true)) {
            return emptyList()
          }
          return parseChpl(buf)
        }

        else -> {
          // just skip this whole atom
          if (!input.skipFully(payloadSize, true)) {
            return emptyList()
          }
        }
      }

      // 3) ensure we’re at the end of this box
      if (input.position < payloadEnd) {
        if (!input.skipFully((payloadEnd - input.position).toInt(), true)) {
          return emptyList()
        }
      }
    }

    // no chpl found in this branch
    return emptyList()
  }

  private fun parseChpl(data: ParsableByteArray): List<MarkData> {
    data.setPosition(0)
    val version = data.readUnsignedByte()
    data.skipBytes(3) // flags
    data.skipBytes(1) // reserved
    val count = data.readUnsignedIntToInt()

    val results = mutableListOf<MarkData>()
    repeat(count) {
      val start = if (version == 0) data.readUnsignedInt() else data.readUnsignedLongToLong()
      val titleLen = data.readUnsignedByte()
      val title = data.readString(titleLen)
      val ms = (start / 10_000)
      results += MarkData(startMs = ms, name = title)
    }
    return results
  }
}
