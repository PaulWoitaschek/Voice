package voice.app.scanner.mp4

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.extractor.DefaultExtractorInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.data.MarkData
import voice.logging.core.Logger
import java.io.IOException
import javax.inject.Inject

class Mp4ChapterExtractor
@Inject constructor(
  private val context: Context,
  private val boxParser: Mp4BoxParser,
) {

  suspend fun extractChapters(uri: Uri): List<MarkData> = withContext(Dispatchers.IO) {
    val dataSource = DefaultDataSource.Factory(context).createDataSource()

    try {
      dataSource.open(DataSpec(uri))
      val input = DefaultExtractorInput(dataSource, 0, C.LENGTH_UNSET.toLong())
      val topLevelResult = boxParser.parse(input)
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
}
