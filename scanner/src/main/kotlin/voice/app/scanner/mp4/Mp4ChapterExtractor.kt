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
import voice.app.scanner.mp4.visitor.ChapVisitor
import voice.app.scanner.mp4.visitor.ChplVisitor
import voice.app.scanner.mp4.visitor.MdhdVisitor
import voice.app.scanner.mp4.visitor.StcoVisitor
import voice.app.scanner.mp4.visitor.StscVisitor
import voice.app.scanner.mp4.visitor.SttsVisitor
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
@Inject constructor(
  private val context: Context,
  stscVisitor: StscVisitor,
  mdhdVisitor: MdhdVisitor,
  sttsVisitor: SttsVisitor,
  stcoVisitor: StcoVisitor,
  chplVisitor: ChplVisitor,
  chapVisitor: ChapVisitor,
) {

  private val visitors = listOf(
    stscVisitor,
    mdhdVisitor,
    sttsVisitor,
    stcoVisitor,
    chplVisitor,
    chapVisitor,
  )
  private val visitorByPath = visitors.associateBy { it.path }

  suspend fun extractChapters(uri: Uri): List<MarkData> = withContext(Dispatchers.IO) {
    val dataSource = DefaultDataSource.Factory(context).createDataSource()

    try {
      dataSource.open(DataSpec(uri))
      val input = DefaultExtractorInput(dataSource, 0, C.LENGTH_UNSET.toLong())
      val topLevelResult = parseTopLevelBoxes(input)
      val trackId = topLevelResult.chapterTrackId
      when {
        topLevelResult.chplChapters.isNotEmpty() -> {
          topLevelResult.chplChapters
        }
        trackId != null -> {
          extractFromTrackId(uri, dataSource, trackId, topLevelResult)
        }
        else -> emptyList()
      }
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

  private fun parseTopLevelBoxes(input: ExtractorInput): Mp4ChpaterExtractorOutput {
    val scratch = ParsableByteArray(Mp4Box.LONG_HEADER_SIZE)
    val parseOutput = Mp4ChpaterExtractorOutput()
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
    output: Mp4ChpaterExtractorOutput,
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

  private fun getSamplesPerChunk(
    chunkIndex: Int,
    stscEntries: List<StscEntry>,
  ): Int {
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
    parseOutput: Mp4ChpaterExtractorOutput,
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

      val visitor = visitorByPath[currentPath]

      when {
        visitor != null -> {
          Logger.v("Found ${visitor.path.last()}!")
          val buffer = ParsableByteArray(payloadSize)
          if (!input.readFully(buffer.data, 0, payloadSize, true)) {
            return
          }
          visitor.visit(buffer, parseOutput)

          if (parseOutput.chplChapters.isNotEmpty()) {
            return
          }
        }

        visitors.any { it.path.startsWith(currentPath) } -> {
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
}

private fun List<String>.startsWith(other: List<String>): Boolean {
  return take(other.size) == other
}
