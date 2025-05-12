package voice.app.scanner.mp4

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.container.Mp4Box
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.ExtractorInput
import voice.data.MarkData
import voice.logging.core.Logger
import java.io.IOException

internal sealed interface Result {
  data class FoundChplChapters(val chapters: List<MarkData>) : Result
  data class FoundChapTrackId(val trackId: Int) : Result
}

suspend fun parse(
  context: Context,
  uri: Uri,
): List<MarkData> {
  val dataSourceFactory = DefaultDataSource.Factory(context)
  val dataSource = dataSourceFactory.createDataSource()
  try {
    dataSource.open(DataSpec(uri))
  } catch (e: IOException) {
    Logger.d(e)
    return emptyList()
  }
  val input = DefaultExtractorInput(dataSource, 0, C.LENGTH_UNSET.toLong())

  val result = parse8(input)
  return when (result) {
    is Result.FoundChapTrackId -> {
      extractChapterCuesDirectly(context, uri, result.trackId)
    }
    is Result.FoundChplChapters -> result.chapters
    null -> emptyList()
  }
}

internal fun parse8(input: ExtractorInput): Result? {
  val scratch = ParsableByteArray(Mp4Box.LONG_HEADER_SIZE)
  return try {
    parseBoxes(input = input, depth = 0, parentEnd = Long.MAX_VALUE, scratch = scratch)
  } catch (e: IOException) {
    Logger.w(e, "Failed to parse mp4 chapters")
    null
  } catch (e: IllegalStateException) {
    Logger.w(e, "Failed to parse mp4 chapters")
    null
  }
}

private fun parseBoxes(
  input: ExtractorInput,
  depth: Int,
  parentEnd: Long,
  scratch: ParsableByteArray,
): Result? {
  var chapTrackId: Result.FoundChapTrackId? = null

  while (input.position < parentEnd) {
    // [existing atom header parsing code]
    scratch.reset(Mp4Box.HEADER_SIZE)
    if (!input.readFully(scratch.data, 0, Mp4Box.HEADER_SIZE, true)) {
      return chapTrackId
    }
    scratch.setPosition(0)
    var atomSize = scratch.readUnsignedInt()
    val atomType = scratch.readString(4)
    var headerSize = Mp4Box.HEADER_SIZE

    // [existing extended size code]
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
      ((depth == 0 && atomType == "moov") || (depth == 1 && atomType in listOf("udta", "trak")) || depth == 2 && atomType == "tref") -> {
        val result = parseBoxes(
          input = input,
          depth = depth + 1,
          parentEnd = payloadEnd,
          scratch = scratch,
        )
        // Immediately return if we found chpl chapters
        if (result is Result.FoundChplChapters) return result
        // Otherwise save the trackId result but keep searching for chpl chapters
        if (result is Result.FoundChapTrackId) chapTrackId = result
      }
      depth == 2 && atomType == "chpl" -> {
        val buf = ParsableByteArray(payloadSize)
        if (!input.readFully(buf.data, 0, payloadSize, true)) {
          return chapTrackId
        }
        val chapters = parseChpl(buf)
        if (chapters.isNotEmpty()) {
          // chpl chapters take priority - return immediately
          return Result.FoundChplChapters(chapters)
        }
      }
      atomType == "chap" -> {
        val buf = ParsableByteArray(payloadSize)
        if (!input.readFully(buf.data, 0, payloadSize, true)) {
          return chapTrackId
        }
        val trackId = buf.readUnsignedIntToInt()
        // Store but don't return yet, in case we find chpl chapters
        chapTrackId = Result.FoundChapTrackId(trackId)
      }

      else -> {
        // just skip this whole atom
        if (!input.skipFully(payloadSize, true)) {
          return chapTrackId
        }
      }
    }

    // 3) ensure we're at the end of this box
    if (input.position < payloadEnd) {
      if (!input.skipFully((payloadEnd - input.position).toInt(), true)) {
        return chapTrackId
      }
    }
  }

  // Return chap trackId if found, otherwise null
  return chapTrackId
}

private fun parseChpl(data: ParsableByteArray): List<MarkData> {
  data.setPosition(0)
  val version = data.readUnsignedByte()
  data.skipBytes(3) // flags
  data.skipBytes(1) // reserved
  val count = data.readUnsignedIntToInt()

  return (0 until count).map {
    val start = if (version == 0) data.readUnsignedInt() else data.readUnsignedLongToLong()
    val titleLen = data.readUnsignedByte()
    val title = data.readString(titleLen)
    val ms = (start / 10_000)
    MarkData(startMs = ms, name = title)
  }
}
