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
    var durationEntryIndex = 0
    var samplesConsumedInDurationEntry = 0L

    return (0 until numberOfChaptersToProcess)
      .map { chunkIndex ->
        val chapterName = names[chunkIndex]

        val samplesInThisChunk = getSamplesPerChunk(chunkIndex, stscEntries)
        val chunkDuration = consumeDuration(
          sampleCount = samplesInThisChunk,
          durations = durations,
          durationEntryIndex = durationEntryIndex,
          samplesConsumedInDurationEntry = samplesConsumedInDurationEntry,
        )
        durationEntryIndex = chunkDuration.durationEntryIndex
        samplesConsumedInDurationEntry = chunkDuration.samplesConsumedInDurationEntry
        if (!chunkDuration.hasEnoughDurations) {
          Logger.w("Not enough sample durations for chunk ${chunkIndex + 1}")
        }

        MarkData(
          startMs = position * 1000 / timeScale,
          name = chapterName,
        ).also {
          position += chunkDuration.duration
        }
      }
      .sorted()
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
