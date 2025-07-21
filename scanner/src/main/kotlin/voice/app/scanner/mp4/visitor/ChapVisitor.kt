package voice.app.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import dev.zacsweers.metro.Inject
import voice.app.scanner.mp4.Mp4ChpaterExtractorOutput

@Inject
class ChapVisitor : AtomVisitor {

  override val path: List<String> = listOf("moov", "trak", "tref", "chap")

  override fun visit(
    buffer: ParsableByteArray,
    parseOutput: Mp4ChpaterExtractorOutput,
  ) {
    val trackId = buffer.readUnsignedIntToInt()
    parseOutput.chapterTrackId = trackId
  }
}
