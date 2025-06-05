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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.data.MarkData
import voice.logging.core.Logger
import java.io.IOException
import javax.inject.Inject

/**
 * Extracts chapter information from MP4 files using two methods:
 *
 * 1. Primary method: Search for 'chpl' atom in MP4 structure, which directly contains chapter data
 * 2. Fallback method: If 'chap' atom is found, extract chapter info from the referenced text track
 *
 * The extraction process:
 * - Opens MP4 file and traverses its box structure (moov → udta/trak → chpl/tref/chap)
 * - If 'chpl' atom is found, parses timestamps and titles directly
 * - If 'chap' atom is found, uses ChapterTrackExtractor to process the referenced text track
 * - Returns chapters as MarkData objects with timestamps and names
 */
class Mp4ChapterExtractor @Inject constructor(
  private val context: Context,
  private val chapterTrackExtractor: ChapterTrackExtractor,
) {

  suspend fun extractChapters(uri: Uri): List<MarkData> = withContext(Dispatchers.IO) {
    val dataSource = DefaultDataSource.Factory(context).createDataSource()

    try {
      dataSource.open(DataSpec(uri))

      val input = DefaultExtractorInput(dataSource, 0, C.LENGTH_UNSET.toLong())
      when (val result = parseTopLevelBoxes(input)) {
        is ChapterParseResult.ChplChapters -> result.chapters
        is ChapterParseResult.ChapterTrackId -> {
          chapterTrackExtractor.extractFromTrackId(uri, result.trackId)
        }

        null -> emptyList()
      }
    } catch (e: IOException) {
      Logger.w(e, "Failed to open MP4 file for chapter extraction")
      emptyList()
    } finally {
      try {
        dataSource.close()
      } catch (e: IOException) {
        Logger.w(e, "Error closing data source")
      }
    }
  }

  private fun parseTopLevelBoxes(input: ExtractorInput): ChapterParseResult? {
    val scratch = ParsableByteArray(Mp4Box.LONG_HEADER_SIZE)
    return try {
      parseBoxes(
        input = input,
        path = emptyList(),
        parentEnd = Long.MAX_VALUE,
        scratch = scratch,
        parseOutput = BoxParseOutput(),
      )
    } catch (e: IOException) {
      Logger.w(e, "Failed to parse MP4 boxes")
      null
    } catch (e: IllegalStateException) {
      Logger.w(e, "Invalid MP4 structure")
      null
    } catch (e: ArrayIndexOutOfBoundsException) {
      Logger.w(e, "Undeclared")
      // https://github.com/androidx/media/issues/2467
      null
    }
  }

  private data class BoxParseOutput(
    val chunkOffsets: MutableList<List<Long>> = mutableListOf(),
    val durations: MutableList<List<Long>> = mutableListOf(),
    val timeScales: MutableList<Long> = mutableListOf<Long>(),
  )

  private fun parseBoxes(
    input: ExtractorInput,
    path: List<String>,
    parentEnd: Long,
    scratch: ParsableByteArray,
    parseOutput: BoxParseOutput,
  ): ChapterParseResult? {
    var chapterTrackId: ChapterParseResult.ChapterTrackId? = null

    while (input.position < parentEnd) {
      scratch.reset(Mp4Box.HEADER_SIZE)
      if (!input.readFully(scratch.data, 0, Mp4Box.HEADER_SIZE, true)) {
        return chapterTrackId
      }

      scratch.setPosition(0)
      var atomSize = scratch.readUnsignedInt()
      val atomType = scratch.readString(4)
      var headerSize = Mp4Box.HEADER_SIZE

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
      val currentPath = path + atomType
      Logger.d("Current path: $currentPath, atomType: $atomType")
      val pathsToVisit = listOf(
        chplPath,
        chapPath,
        mdhdPath,
        stcoPath,
        sttsPath,
      )
      when {
        currentPath == mdhdPath -> {
          // https://developer.apple.com/documentation/quicktime-file-format/media_header_atom
          Logger.d("Found mdhd!")
          val buffer = ParsableByteArray(payloadSize)
          if (!input.readFully(buffer.data, 0, payloadSize, true)) {
            return chapterTrackId
          }
          val version = buffer.readUnsignedByte()
          if (version != 0 && version != 1) {
            Logger.w("Unexpected version $version in mdhd atom, expected 0 or 1")
            return chapterTrackId
          }
          val flagsSize = 3
          val creationTimeSize = if (version == 0) 4 else 8
          val modificationTimeSize = if (version == 0) 4 else 8
          buffer.skipBytes(flagsSize + creationTimeSize + modificationTimeSize)
          val timescale = buffer.readUnsignedInt()
          Logger.d("Timescale: $timescale")
          parseOutput.timeScales += timescale
        }
        currentPath == sttsPath -> {
          Logger.d("Found stts!")
          val buffer = ParsableByteArray(payloadSize)
          if (!input.readFully(buffer.data, 0, payloadSize, true)) {
            return chapterTrackId
          }
          val version = buffer.readUnsignedByte()
          if (version != 0) {
            Logger.w("Unexpected version $version in stts atom, expected 0")
            return chapterTrackId
          }
          buffer.skipBytes(3) // flags
          val numberOfEntries = buffer.readUnsignedIntToInt()
          val durations = (0 until numberOfEntries).map {
            val count = buffer.readUnsignedInt()
            val delta = buffer.readUnsignedInt()
            1000L * count * delta
          }
          //     Logger.d("Durations: $durations")
          parseOutput.durations += durations
        }

        currentPath == stcoPath -> {
          Logger.d("Found stco!")
          val buffer = ParsableByteArray(payloadSize)
          if (!input.readFully(buffer.data, 0, payloadSize, true)) {
            return chapterTrackId
          }
          // https://developer.apple.com/documentation/quicktime-file-format/chunk_offset_atom
          val version = buffer.readUnsignedByte()
          if (version != 0) {
            Logger.w("Unexpected version $version in stco atom, expected 0")
            return chapterTrackId
          }
          buffer.skipBytes(3) // flags
          val numberOfEntries = buffer.readUnsignedIntToInt()
          val chunkOffsets = (0 until numberOfEntries).map { buffer.readUnsignedInt() }
          //  Logger.d("ChunkOffsets: $chunkOffsets")
          parseOutput.chunkOffsets.add(chunkOffsets)
        }
        currentPath == chplPath -> {
          val buffer = ParsableByteArray(payloadSize)
          if (!input.readFully(buffer.data, 0, payloadSize, true)) {
            return chapterTrackId
          }

          val chapters = parseChplAtom(buffer)
          if (chapters.isNotEmpty()) {
            return ChapterParseResult.ChplChapters(chapters)
          }
        }

        currentPath == chapPath -> {
          val buffer = ParsableByteArray(payloadSize)
          if (!input.readFully(buffer.data, 0, payloadSize, true)) {
            return chapterTrackId
          }

          val trackId = buffer.readUnsignedIntToInt()
          chapterTrackId = ChapterParseResult.ChapterTrackId(trackId)
        }

        (pathsToVisit.any { it.startsWith(currentPath) }) -> {
          val result = parseBoxes(
            input = input,
            path = currentPath,
            parentEnd = payloadEnd,
            scratch = scratch,
            parseOutput = parseOutput,
          )

          if (result is ChapterParseResult.ChplChapters) {
            return result
          }

          if (result is ChapterParseResult.ChapterTrackId) {
            chapterTrackId = result
          }
        }

        else -> {
          if (!input.skipFully(payloadSize, true)) {
            return chapterTrackId
          }
        }
      }

      if (input.position < payloadEnd) {
        if (!input.skipFully((payloadEnd - input.position).toInt(), true)) {
          return chapterTrackId
        }
      }
    }

    return chapterTrackId
  }

  private fun parseChplAtom(data: ParsableByteArray): List<MarkData> {
    data.setPosition(0)
    val version = data.readUnsignedByte()
    data.skipBytes(3) // flags
    data.skipBytes(1) // reserved
    val chapterCount = data.readUnsignedIntToInt()

    val chapters = mutableListOf<MarkData>()
    repeat(chapterCount) {
      val timestamp = if (version == 0) {
        data.readUnsignedInt()
      } else {
        data.readUnsignedLongToLong()
      }

      val titleLength = data.readUnsignedByte()
      val title = data.readString(titleLength)

      // Convert from 100ns units to milliseconds
      val startTimeMs = timestamp / 10_000
      chapters.add(MarkData(startMs = startTimeMs, name = title))
    }

    return chapters
  }

  private sealed class ChapterParseResult {
    data class ChplChapters(val chapters: List<MarkData>) : ChapterParseResult()
    data class ChapterTrackId(val trackId: Int) : ChapterParseResult()
  }
}

private val chplPath = listOf("moov", "udta", "chpl")
private val chapPath = listOf("moov", "trak", "tref", "chap")
private val mdhdPath = listOf("moov", "trak", "mdia", "mdhd")
private val stcoPath = listOf("moov", "trak", "mdia", "minf", "stbl", "stco")
private val sttsPath = listOf("moov", "trak", "mdia", "minf", "stbl", "stts")

private fun List<String>.startsWith(other: List<String>): Boolean {
  return take(other.size) == other
}
