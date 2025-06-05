package voice.app.scanner.mp4

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.container.Mp4Box
import androidx.media3.datasource.DataSource
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
class Mp4ChapterExtractor
@Inject constructor(private val context: Context) {

  suspend fun extractChapters(uri: Uri): List<MarkData> = withContext(Dispatchers.IO) {
    val dataSource = DefaultDataSource.Factory(context).createDataSource()

    try {
      dataSource.open(DataSpec(uri))
      val input = DefaultExtractorInput(dataSource, 0, C.LENGTH_UNSET.toLong())
      val topLevelResult = parseTopLevelBoxes(input)
      if (topLevelResult.chplChapters.isNotEmpty()) {
        return@withContext topLevelResult.chplChapters
      }
      val trackId = topLevelResult.chapterTrackId
      if (trackId != null) {
        return@withContext extractFromTrackId(uri, dataSource, trackId, topLevelResult)
      }
      return@withContext emptyList()
    } catch (e: IOException) {
      Logger.w(e, "Failed to open MP4 file for chapter extraction")
      emptyList()
    } catch (e: IllegalStateException) {
      Logger.w(e, "Invalid MP4 structure")
      emptyList()
    } catch (e: ArrayIndexOutOfBoundsException) {
      Logger.w(e, "Undeclared")
      // https://github.com/androidx/media/issues/2467
      emptyList()
    } finally {
      try {
        dataSource.close()
      } catch (e: IOException) {
        Logger.w(e, "Error closing data source")
      }
    }
  }

  private fun parseTopLevelBoxes(input: ExtractorInput): BoxParseOutput {
    val scratch = ParsableByteArray(Mp4Box.LONG_HEADER_SIZE)
    val parseOutput = BoxParseOutput()
    parseBoxes(
      input = input,
      path = emptyList(),
      parentEnd = Long.MAX_VALUE,
      scratch = scratch,
      parseOutput = parseOutput,
    )
    return parseOutput
  }

  private fun extractFromTrackId(
    uri: Uri,
    dataSource: DataSource,
    trackId: Int,
    output: BoxParseOutput,
  ): List<MarkData> {
    val chunkOffsets = output.chunkOffsets.getOrNull(trackId - 1)
    if (chunkOffsets == null) {
      Logger.w("No chunk offsets found for track ID $trackId")
      return emptyList()
    }
    val timeScale = output.timeScales.getOrNull(trackId - 1)
    if (timeScale == null) {
      Logger.w("No time scale found for track ID $trackId")
      return emptyList()
    }
    val durations = output.durations.getOrNull(trackId - 1)
    if (durations == null) {
      Logger.w("No durations found for track ID $trackId")
      return emptyList()
    }
    val stscEntries = output.stscEntries.getOrNull(trackId - 1)
    if (stscEntries == null) {
      Logger.w("No stsc entries found for track ID $trackId")
      return emptyList()
    }

    val numberOfChaptersToProcess = chunkOffsets.size

    val names = chunkOffsets.map { offset ->
      dataSource.close()
      dataSource.open(
        DataSpec.Builder()
          .setUri(uri)
          .setPosition(offset)
          .build(),
      )
      val buffer = ParsableByteArray()
      buffer.reset(2)
      dataSource.read(buffer.data, 0, 2)
      val textLength = buffer.readShort().toInt()
      buffer.reset(textLength)
      dataSource.read(buffer.data, 0, textLength)
      buffer.readString(textLength)
    }

    if (names.size != numberOfChaptersToProcess) {
      Logger.w("Mismatch in names size and chunk offsets size for track ID $trackId")
      return emptyList()
    }

    var position = 0L
    var sampleIndex = 0

    return (0 until numberOfChaptersToProcess)
      .map { chunkIndex ->
        val chapterName = names[chunkIndex]

        val samplesInThisChunk = getSamplesPerChunk(chunkIndex, stscEntries)

        var chunkDuration = 0L

        repeat(samplesInThisChunk) {
          if (sampleIndex < durations.size) {
            chunkDuration += durations[sampleIndex]
            sampleIndex++
          } else {
            Logger.w("Not enough sample durations for chunk ${chunkIndex + 1}")
          }
        }

        MarkData(
          startMs = position * 1000 / timeScale,
          name = chapterName,
        ).also {
          position += chunkDuration
        }
      }
      .sorted()
  }

  private fun getSamplesPerChunk(chunkIndex: Int, stscEntries: List<StscEntry>): Int {
    val chunkNumber = chunkIndex + 1

    for (i in stscEntries.indices) {
      val entry = stscEntries[i]
      val nextEntry = stscEntries.getOrNull(i + 1)

      if (chunkNumber >= entry.firstChunk) {
        if (nextEntry == null || chunkNumber < nextEntry.firstChunk) {
          return entry.samplesPerChunk
        }
      }
    }

    return 1
  }

  private fun parseBoxes(
    input: ExtractorInput,
    path: List<String>,
    parentEnd: Long,
    scratch: ParsableByteArray,
    parseOutput: BoxParseOutput,
  ) {
    while (input.position < parentEnd) {
      scratch.reset(Mp4Box.HEADER_SIZE)
      if (!input.readFully(scratch.data, 0, Mp4Box.HEADER_SIZE, true)) {
        return
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
      when {
        currentPath == mdhdPath -> {
          Logger.v("Found mdhd!")
          val buffer = ParsableByteArray(payloadSize)
          if (!input.readFully(buffer.data, 0, payloadSize, true)) {
            return
          }
          parseMdhdAtom(buffer, parseOutput)
        }
        currentPath == sttsPath -> {
          Logger.v("Found stts!")
          val buffer = ParsableByteArray(payloadSize)
          if (!input.readFully(buffer.data, 0, payloadSize, true)) {
            return
          }
          parseSttsAtom(buffer, parseOutput)
        }
        currentPath == stcoPath -> {
          Logger.v("Found stco!")
          val buffer = ParsableByteArray(payloadSize)
          if (!input.readFully(buffer.data, 0, payloadSize, true)) {
            return
          }
          parseStcoAtom(buffer, parseOutput)
        }
        currentPath == stscPath -> {
          Logger.v("Found stsc!")
          val buffer = ParsableByteArray(payloadSize)
          if (!input.readFully(buffer.data, 0, payloadSize, true)) {
            return
          }
          parseStscAtom(buffer, parseOutput)
        }
        currentPath == chplPath -> {
          val buffer = ParsableByteArray(payloadSize)
          if (!input.readFully(buffer.data, 0, payloadSize, true)) {
            return
          }

          val chapters = parseChplAtom(buffer)
          parseOutput.chplChapters = chapters
          if (chapters.isNotEmpty()) {
            return
          }
        }

        currentPath == chapPath -> {
          val buffer = ParsableByteArray(payloadSize)
          if (!input.readFully(buffer.data, 0, payloadSize, true)) {
            return
          }

          val trackId = buffer.readUnsignedIntToInt()
          parseOutput.chapterTrackId = trackId
        }

        pathsToVisit.any { it.startsWith(currentPath) } -> {
          parseBoxes(
            input = input,
            path = currentPath,
            parentEnd = payloadEnd,
            scratch = scratch,
            parseOutput = parseOutput,
          )

          if (parseOutput.chplChapters.isNotEmpty()) {
            return
          }
        }

        else -> {
          if (!input.skipFully(payloadSize, true)) {
            return
          }
        }
      }

      if (input.position < payloadEnd) {
        if (!input.skipFully((payloadEnd - input.position).toInt(), true)) {
          return
        }
      }
    }
  }

  // https://developer.apple.com/documentation/quicktime-file-format/chunk_offset_atom
  private fun parseStcoAtom(
    buffer: ParsableByteArray,
    parseOutput: BoxParseOutput,
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

  // https://developer.apple.com/documentation/quicktime-file-format/time-to-sample_atom
  private fun parseSttsAtom(
    buffer: ParsableByteArray,
    parseOutput: BoxParseOutput,
  ) {
    val version = buffer.readUnsignedByte()
    if (version != 0) {
      Logger.w("Unexpected version $version in stts atom, expected 0")
    } else {
      buffer.skipBytes(3) // flags
      val numberOfEntriesInSttsTable = buffer.readUnsignedIntToInt()
      Logger.v("Number of entries in stts: $numberOfEntriesInSttsTable")
      val individualSampleDurations = mutableListOf<Long>()
      repeat(numberOfEntriesInSttsTable) {
        val count = buffer.readUnsignedInt().toInt()
        val delta = buffer.readUnsignedInt()
        repeat(count) {
          individualSampleDurations.add(delta)
        }
      }
      parseOutput.durations.add(individualSampleDurations)
    }
  }

  // https://developer.apple.com/documentation/quicktime-file-format/sample-to-chunk_atom/
  private fun parseStscAtom(
    buffer: ParsableByteArray,
    parseOutput: BoxParseOutput,
  ) {
    val version = buffer.readUnsignedByte()
    if (version != 0) {
      Logger.w("Unexpected version $version in stsc atom, expected 0")
    } else {
      buffer.skipBytes(3) // flags
      val numberOfEntries = buffer.readUnsignedIntToInt()
      Logger.v("Number of entries in stsc: $numberOfEntries")
      val stscEntriesForTrack = (0 until numberOfEntries).map {
        val firstChunk = buffer.readUnsignedInt()
        val samplesPerChunk = buffer.readUnsignedIntToInt()
        buffer.skipBytes(4) // skip sample description index
        StscEntry(
          firstChunk = firstChunk,
          samplesPerChunk = samplesPerChunk,
        )
      }
      parseOutput.stscEntries.add(stscEntriesForTrack)
    }
  }

  // https://developer.apple.com/documentation/quicktime-file-format/media_header_atom
  private fun parseMdhdAtom(
    buffer: ParsableByteArray,
    parseOutput: BoxParseOutput,
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

  private fun parseChplAtom(data: ParsableByteArray): List<MarkData> {
    data.setPosition(0)
    val version = data.readUnsignedByte()
    data.skipBytes(3)  // flags

    if (version != 0 && version != 1) {
      Logger.w("Unexpected version $version in chpl atom, expected 0 or 1")
      return emptyList()
    }

    if (version == 1) {
      data.skipBytes(4)
    }

    val chapterCount = data.readUnsignedByte()

    return (0 until chapterCount).map {
      val timestamp = if (version == 0) {
        data.readUnsignedInt()
      } else {
        data.readUnsignedLongToLong()
      }

      val titleLength = data.readUnsignedByte()
      val title = data.readString(titleLength)

      // Convert from 100ns units to milliseconds (10,000 units per ms)
      val startTimeMs = timestamp / 10_000
      MarkData(startMs = startTimeMs, name = title)
    }
  }
}

private val chplPath = listOf("moov", "udta", "chpl")
private val chapPath = listOf("moov", "trak", "tref", "chap")
private val mdhdPath = listOf("moov", "trak", "mdia", "mdhd")
private val stcoPath = listOf("moov", "trak", "mdia", "minf", "stbl", "stco")
private val sttsPath = listOf("moov", "trak", "mdia", "minf", "stbl", "stts")
private val stscPath = listOf("moov", "trak", "mdia", "minf", "stbl", "stsc")
private val pathsToVisit = listOf(
  chplPath,
  chapPath,
  mdhdPath,
  stcoPath,
  sttsPath,
  stscPath,
)

private fun List<String>.startsWith(other: List<String>): Boolean {
  return take(other.size) == other
}

private data class StscEntry(
  val firstChunk: Long,
  val samplesPerChunk: Int,
)

private data class BoxParseOutput(
  val chunkOffsets: MutableList<List<Long>> = mutableListOf(),
  val durations: MutableList<List<Long>> = mutableListOf(),
  val stscEntries: MutableList<List<StscEntry>> = mutableListOf(),
  val timeScales: MutableList<Long> = mutableListOf(),
  var chplChapters: List<MarkData> = emptyList(),
  var chapterTrackId: Int? = null,
)
