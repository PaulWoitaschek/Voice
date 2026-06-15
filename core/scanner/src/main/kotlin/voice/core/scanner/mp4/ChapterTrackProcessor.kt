package voice.core.scanner.mp4

import android.net.Uri
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import dev.zacsweers.metro.Inject
import voice.core.data.MarkData
import voice.core.logging.api.Logger

@Inject
internal class ChapterTrackProcessor {

  operator fun invoke(
    uri: Uri,
    dataSource: DataSource,
    trackId: Int,
    output: Mp4ChpaterExtractorOutput,
  ): List<MarkData> {
    val trackIndex =
      output.trackIds.indexOf(trackId)

    if (trackIndex == -1) {
      Logger.w("Track ID $trackId not found")
      return emptyList()
    }
    val chunkOffsets = output.chunkOffsets.getOrNull(trackIndex)

    if (chunkOffsets == null) {
      Logger.w("No chunk offsets found for track ID $trackId")
      return emptyList()
    }

    val timeScale = output.timeScales.getOrNull(trackIndex)

    if (timeScale == null) {
      Logger.w("No time scale found for track ID $trackId")
      return emptyList()
    }

    val durations = output.durations.getOrNull(trackIndex)

    if (durations == null) {
      Logger.w("No durations found for track ID $trackId")
      return emptyList()
    }

    val stscEntries = output.stscEntries.getOrNull(trackIndex)

    if (stscEntries == null) {
      Logger.w("No stsc entries found for track ID $trackId")
      return emptyList()
    }
    val sampleSizes = output.sampleSizes.getOrNull(trackIndex)

    if (sampleSizes == null) {
      Logger.w("No sample sizes found for track ID $trackId")
      return emptyList()
    }

    val chapterChunkOffset = chunkOffsets.first()

    dataSource.close()
    dataSource.open(
      DataSpec.Builder()
        .setUri(uri)
        .setPosition(chapterChunkOffset)
        .build(),
    )

    val names =
      sampleSizes.mapIndexed { index, sampleSize ->
        val sampleBuffer = ParsableByteArray()
        sampleBuffer.reset(sampleSize)
        dataSource.read(sampleBuffer.data, 0, sampleSize)
        val textLength = sampleBuffer.readShort().toInt()
        val name = sampleBuffer.readString(textLength)
        name
      }

    val numberOfChaptersToProcess = names.size
    var position = 0L
    var durationEntryIndex = 0
    var samplesConsumedInDurationEntry = 0L

    val result = (0 until numberOfChaptersToProcess)
      .map { chunkIndex ->
        val chapterName = names[chunkIndex]

        val samplesInThisChunk =
          if (chunkOffsets.size == 1 && sampleSizes.size == durations.size) {
            1
          } else {
            getSamplesPerChunk(chunkIndex, stscEntries)
          }
        val chunkDuration = consumeDuration(
          sampleCount = samplesInThisChunk,
          durations = durations,
          durationEntryIndex = durationEntryIndex,
          samplesConsumedInDurationEntry = samplesConsumedInDurationEntry,
        )

        durationEntryIndex = chunkDuration.durationEntryIndex
        samplesConsumedInDurationEntry = chunkDuration.samplesConsumedInDurationEntry
        MarkData(
          startMs = position * 1000 / timeScale,
          name = chapterName,
        ).also {
          position += chunkDuration.duration
        }
      }
      .sorted()
    return result
  }

  private fun consumeDuration(
    sampleCount: Int,
    durations: List<SttsEntry>,
    durationEntryIndex: Int,
    samplesConsumedInDurationEntry: Long,
  ): ConsumedDuration {
    var remainingSamples = sampleCount.toLong()
    var entryIndex = durationEntryIndex
    var consumedInEntry = samplesConsumedInDurationEntry
    var duration = 0L

    while (remainingSamples > 0) {
      val entry = durations.getOrNull(entryIndex)
        ?: return ConsumedDuration(
          duration = duration,
          durationEntryIndex = entryIndex,
          samplesConsumedInDurationEntry = consumedInEntry,
          hasEnoughDurations = false,
        )

      val samplesLeftInEntry = entry.sampleCount - consumedInEntry
      if (samplesLeftInEntry <= 0) {
        entryIndex++
        consumedInEntry = 0
      } else {
        val samplesToConsume = minOf(remainingSamples, samplesLeftInEntry)
        duration += samplesToConsume * entry.sampleDuration
        remainingSamples -= samplesToConsume
        consumedInEntry += samplesToConsume
      }
    }

    return ConsumedDuration(
      duration = duration,
      durationEntryIndex = entryIndex,
      samplesConsumedInDurationEntry = consumedInEntry,
      hasEnoughDurations = true,
    )
  }

  private fun getSamplesPerChunk(
    chunkIndex: Int,
    stscEntries: List<StscEntry>,
  ): Int {
    stscEntries.forEachIndexed { index, entry ->
      val nextEntry = stscEntries.getOrNull(index + 1)
      if (chunkIndex + 1 >= entry.firstChunk) {
        if (nextEntry == null || chunkIndex + 1 < nextEntry.firstChunk) {
          return entry.samplesPerChunk
        }
      }
    }
    return 1
  }

  private data class ConsumedDuration(
    val duration: Long,
    val durationEntryIndex: Int,
    val samplesConsumedInDurationEntry: Long,
    val hasEnoughDurations: Boolean,
  )
}
