package voice.app.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import voice.app.scanner.mp4.Mp4ChpaterExtractorOutput
import javax.inject.Inject

class ChapVisitor @Inject constructor() : AtomVisitor {

  override val path: List<String> = listOf("moov", "trak", "tref", "chap")

  override fun visit(
    buffer: ParsableByteArray,
    parseOutput: Mp4ChpaterExtractorOutput,
  ) {
    val trackId = buffer.readUnsignedIntToInt()
    parseOutput.chapterTrackId = trackId
  }
}
