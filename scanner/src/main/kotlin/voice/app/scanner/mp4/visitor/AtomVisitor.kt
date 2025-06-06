package voice.app.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import voice.app.scanner.mp4.Mp4ChpaterExtractorOutput

interface AtomVisitor {
  val path: List<String>

  fun visit(
    buffer: ParsableByteArray,
    parseOutput: Mp4ChpaterExtractorOutput,
  )
}
