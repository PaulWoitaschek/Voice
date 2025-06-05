package voice.app.scanner.mp4

import androidx.media3.common.util.ParsableByteArray
import androidx.media3.container.Mp4Box
import androidx.media3.extractor.ExtractorInput
import voice.app.scanner.mp4.visitor.ChapVisitor
import voice.app.scanner.mp4.visitor.ChplVisitor
import voice.app.scanner.mp4.visitor.MdhdVisitor
import voice.app.scanner.mp4.visitor.StcoVisitor
import voice.app.scanner.mp4.visitor.StscVisitor
import voice.app.scanner.mp4.visitor.SttsVisitor
import voice.logging.core.Logger
import javax.inject.Inject

class Mp4BoxParser
@Inject constructor(
  stscVisitor: StscVisitor,
  mdhdVisitor: MdhdVisitor,
  sttsVisitor: SttsVisitor,
  stcoVisitor: StcoVisitor,
  chplVisitor: ChplVisitor,
  chapVisitor: ChapVisitor,
) {

  private val visitors = listOf(
    stscVisitor,
    mdhdVisitor,
    sttsVisitor,
    stcoVisitor,
    chplVisitor,
    chapVisitor,
  )
  private val visitorByPath = visitors.associateBy { it.path }

  operator fun invoke(input: ExtractorInput): Mp4ChpaterExtractorOutput {
    val scratch = ParsableByteArray(Mp4Box.LONG_HEADER_SIZE)
    val parseOutput = Mp4ChpaterExtractorOutput()
    parseBoxes(
      input = input,
      path = emptyList(),
      parentEnd = Long.MAX_VALUE,
      scratch = scratch,
      parseOutput = parseOutput,
    )
    return parseOutput
  }

  private fun parseBoxes(
    input: ExtractorInput,
    path: List<String>,
    parentEnd: Long,
    scratch: ParsableByteArray,
    parseOutput: Mp4ChpaterExtractorOutput,
  ) {
    while (input.position < parentEnd) {
      scratch.reset(Mp4Box.HEADER_SIZE)
      if (!input.readFully(scratch.data, 0, Mp4Box.HEADER_SIZE, true)) {
        return
      }

      var atomSize = scratch.readUnsignedInt()
      val atomType = scratch.readString(4)
      var headerSize = Mp4Box.HEADER_SIZE

      if (atomSize == 1L) {
        input.readFully(
          scratch.data,
          Mp4Box.HEADER_SIZE,
          Mp4Box.LONG_HEADER_SIZE - Mp4Box.HEADER_SIZE,
        )
        scratch.setPosition(Mp4Box.HEADER_SIZE)
        atomSize = scratch.readUnsignedLongToLong()
        headerSize = Mp4Box.LONG_HEADER_SIZE
      }

      val payloadSize = (atomSize - headerSize).toInt()
      val payloadEnd = input.position + payloadSize
      val currentPath = path + atomType
      Logger.d("Current path: $currentPath, atomType: $atomType")

      val visitor = visitorByPath[currentPath]

      when {
        visitor != null -> {
          Logger.v("Found ${visitor.path.last()}!")
          scratch.reset(payloadSize)
          if (!input.readFully(scratch.data, 0, payloadSize, true)) {
            return
          }
          visitor.visit(scratch, parseOutput)

          if (parseOutput.chplChapters.isNotEmpty()) {
            return
          }
        }

        visitors.any { it.path.startsWith(currentPath) } -> {
          parseBoxes(
            input = input,
            path = currentPath,
            parentEnd = payloadEnd,
            scratch = scratch,
            parseOutput = parseOutput,
          )

          if (parseOutput.chplChapters.isNotEmpty()) {
            return
          }
        }

        else -> {
          if (!input.skipFully(payloadSize, true)) {
            return
          }
        }
      }

      if (input.position < payloadEnd) {
        if (!input.skipFully((payloadEnd - input.position).toInt(), true)) {
          return
        }
      }
    }
  }

  private fun List<String>.startsWith(other: List<String>): Boolean {
    return take(other.size) == other
  }
}
