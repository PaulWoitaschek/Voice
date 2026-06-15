package voice.core.scanner.mp4

import androidx.media3.common.util.ParsableByteArray
import androidx.media3.container.Mp4Box
import androidx.media3.extractor.ExtractorInput
import dev.zacsweers.metro.Inject
import voice.core.logging.api.Logger
import voice.core.scanner.mp4.visitor.ChapVisitor
import voice.core.scanner.mp4.visitor.ChplVisitor
import voice.core.scanner.mp4.visitor.MdhdVisitor
import voice.core.scanner.mp4.visitor.StcoVisitor
import voice.core.scanner.mp4.visitor.StscVisitor
import voice.core.scanner.mp4.visitor.SttsVisitor
import voice.core.scanner.mp4.visitor.Co64Visitor
import voice.core.scanner.mp4.visitor.TkhdVisitor
import voice.core.scanner.mp4.visitor.StszVisitor

@Inject
internal class Mp4BoxParser(
  stscVisitor: StscVisitor,
  mdhdVisitor: MdhdVisitor,
  sttsVisitor: SttsVisitor,
  stcoVisitor: StcoVisitor,
  co64Visitor: Co64Visitor,
  chplVisitor: ChplVisitor,
  tkhdVisitor: TkhdVisitor,
  chapVisitor: ChapVisitor,
  stszVisitor: StszVisitor,
) {

  private val visitors = listOf(
    stscVisitor,
    mdhdVisitor,
    sttsVisitor,
    stcoVisitor,
    co64Visitor,
    chplVisitor,
    chapVisitor,
    tkhdVisitor,
    stszVisitor,
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
      val payloadSizeLong = atomSize - headerSize

      Logger.e(
        "ATOM DEBUG: atomType=$atomType atomSize=$atomSize headerSize=$headerSize payloadSize=$payloadSizeLong"
      )

      val payloadSize = atomSize - headerSize
      val payloadEnd = input.position + payloadSize
      val currentPath = path + atomType
      Logger.w("Current path: $currentPath, atomType: $atomType")

      val visitor = visitorByPath[currentPath]

      when {
        visitor != null -> {
          Logger.v("Found ${visitor.path.last()}!")

          if (payloadSize > Int.MAX_VALUE) {
            Logger.w("Visitor payload too large: $atomType")
            return
          }

          scratch.reset(payloadSize.toInt())

          if (!input.readFully(
              scratch.data,
              0,
              payloadSize.toInt(),
              true,
            )
          ) {
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
          Logger.e(
            "DEBUG before mdat: chapterTrackId=${parseOutput.chapterTrackId} " +
              "chunkOffsets=${parseOutput.chunkOffsets.size} " +
              "timeScales=${parseOutput.timeScales.size} " +
              "durations=${parseOutput.durations.size} " +
              "stsc=${parseOutput.stscEntries.size}"
          )

          if (atomType == "mdat") {
            Logger.e("STOPPING AT MDAT")
            return
          }

          if (!skipLarge(input, payloadSize)) {
            return
          }
        }
      }
      if (input.position < payloadEnd) {
        val remaining = payloadEnd - input.position

        Logger.e(
          "atom=$atomType currentPath=$currentPath payloadEnd=$payloadEnd position=${input.position} remaining=$remaining"
        )

        if (!skipLarge(input, remaining)) {
          return
        }
      }
    }
  }
  private fun skipLarge(
    input: ExtractorInput,
    bytesToSkip: Long,
  ): Boolean {
    var remaining = bytesToSkip

    while (remaining > 0) {
      val chunk = minOf(remaining, Int.MAX_VALUE.toLong())

      if (!input.skipFully(chunk.toInt(), true)) {
        return false
      }

      remaining -= chunk
    }

    return true
  }

  private fun List<String>.startsWith(other: List<String>): Boolean {
    return take(other.size) == other
  }
}

