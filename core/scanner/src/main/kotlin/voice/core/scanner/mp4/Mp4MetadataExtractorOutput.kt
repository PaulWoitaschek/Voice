package voice.core.scanner.mp4

import voice.core.data.MarkData

internal data class Mp4MetadataExtractorOutput(
  val chunkOffsets: MutableList<List<Long>> = mutableListOf(),
  val durations: MutableList<List<Long>> = mutableListOf(),
  val stscEntries: MutableList<List<StscEntry>> = mutableListOf(),
  val timeScales: MutableList<Long> = mutableListOf(),
  var chplChapters: List<MarkData> = emptyList(),
  var chapterTrackId: Int? = null,
  var series: String? = null,
  var part: String? = null
)

internal data class StscEntry(
  val firstChunk: Long,
  val samplesPerChunk: Int,
)
