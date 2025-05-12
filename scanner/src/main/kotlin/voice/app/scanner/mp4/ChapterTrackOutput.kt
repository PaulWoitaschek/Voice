package voice.app.scanner.mp4

import androidx.media3.common.C
import androidx.media3.extractor.DiscardingTrackOutput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import voice.data.MarkData

internal class ChapterTrackOutput(
  private val targetTrackId: Int,
  private val outputChapters: MutableList<MarkData>,
) : ExtractorOutput {

  private var chapterCueOutput: ChapterCueProcessor? = null

  override fun track(
    id: Int,
    type: Int,
  ): TrackOutput {
    if (type == C.TRACK_TYPE_TEXT && id == targetTrackId) {
      val processor = ChapterCueProcessor(outputChapters)
      if (chapterCueOutput == null) {
        chapterCueOutput = processor
      }
      return processor
    }

    return DiscardingTrackOutput()
  }

  override fun endTracks() {}
  override fun seekMap(seekMap: SeekMap) {}
}
