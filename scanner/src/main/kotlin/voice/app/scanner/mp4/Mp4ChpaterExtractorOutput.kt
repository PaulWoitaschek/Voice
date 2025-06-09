package voice.app.scanner.mp4

import voice.data.MarkData

data class Mp4ChpaterExtractorOutput(
  val chunkOffsets: MutableList<List<Long>> = mutableListOf(),
  val durations: MutableList<List<Long>> = mutableListOf(),
  val stscEntries: MutableList<List<StscEntry>> = mutableListOf(),
  val timeScales: MutableList<Long> = mutableListOf(),
  var chplChapters: List<MarkData> = emptyList(),
  var chapterTrackId: Int? = null,
)

data class StscEntry(
  val firstChunk: Long,
  val samplesPerChunk: Int,
)
