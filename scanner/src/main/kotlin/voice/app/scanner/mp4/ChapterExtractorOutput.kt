package voice.app.scanner.mp4

import androidx.media3.common.C
import androidx.media3.extractor.DiscardingTrackOutput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import voice.data.MarkData

internal class ChapterExtractorOutput(
  private val targetMp4TrackId: Int,
  private val outputCuesList: MutableList<MarkData>,
) : ExtractorOutput {

  private var targetTrackOutput: ChapterCueTrackOutput? = null

  override fun track(
    id: Int,
    type: Int,
  ): TrackOutput {
    if (type == C.TRACK_TYPE_TEXT && id == targetMp4TrackId) {
      val trackOutput = ChapterCueTrackOutput(outputCuesList)
      if (targetTrackOutput == null) {
        targetTrackOutput = trackOutput
      }
      return trackOutput
    }
    return DiscardingTrackOutput()
  }

  override fun endTracks() {}

  override fun seekMap(seekMap: SeekMap) {}
}
