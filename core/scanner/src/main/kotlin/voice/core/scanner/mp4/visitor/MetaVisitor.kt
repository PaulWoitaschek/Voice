package voice.core.scanner.mp4.visitor

import voice.core.logging.core.Logger
import voice.core.scanner.mp4.Mp4MetadataExtractorOutput
import androidx.media3.common.util.ParsableByteArray
import dev.zacsweers.metro.Inject
import java.nio.ByteBuffer




// https://atomicparsley.sourceforge.net/mpeg-4files.html
@Inject
internal class MetaVisitor : AtomVisitor {
  override val path: List<String> = listOf("moov", "udta", "meta")
  val movementIndexField = "©mvi"
  val seriesField = "©mvn"
  val customField = "----"

  var series: String? = null
  var movementIndex: Int? = null
  var part: String? = null

  override fun visit(
    buffer: ParsableByteArray,
    parseOutput: Mp4MetadataExtractorOutput,
  ) {
    val positionBeforeParsing = buffer.position
    // val metaVersionAndFlags = buffer.readString(4)
    buffer.position += 4
    val parentAtom = MetaAtom("meta", buffer.position, buffer.data.size)

    try {
      parseAtoms(buffer, parentAtom)
    } catch(e: Exception) {
      Logger.e(e, "Could not parse atoms in MetaVisitor")
    }

    if(!series.isNullOrBlank()) {
      parseOutput.series = series
    }
    if(!part.isNullOrBlank()) {
      parseOutput.part = part
    }

    if(movementIndex != null && parseOutput.part == null) {
      parseOutput.part = movementIndex.toString()
    }

    buffer.position = positionBeforeParsing
    resetParserValuesToDefaults()
  }

  fun resetParserValuesToDefaults() {
    series = null
    part = null
  }

  fun parseAtoms(buffer: ParsableByteArray, parentAtom: MetaAtom) {
    while(buffer.position < parentAtom.end) {
      val position = buffer.position
      // we can't read beyond the buffer size
      if(buffer.position >= buffer.data.size - 4) {
        break
      }
      val atomSize = buffer.readInt()
      val atomName = buffer.readString(4, Charsets.ISO_8859_1) // this charset is required to correctly read `©`
      val subAtom = MetaAtom(atomName, position, atomSize)
      extractMetaDataField(buffer, parentAtom, atomSize-8)


      if(atomSize > 0 && isAtomNameSupported(atomName) && subAtom.end <= parentAtom.end) {
        parseAtoms(buffer, subAtom)
      } else {
        buffer.position = parentAtom.end
      }
    }
  }

  private fun extractMetaDataField(
    buffer: ParsableByteArray,
    parentAtom: MetaAtom,
    size: Int,
  ) {
    // parentAtom is named and has a data subatom
    when (parentAtom.name) {
      seriesField -> series = parseDataAtomString(buffer, size)
      movementIndexField -> movementIndex = parseDataAtomUnsignedByte(buffer)
      customField -> parseCustomField(buffer, size)
    }
  }

  private fun parseDataAtomUnsignedByte(buffer: ParsableByteArray): Int {
    parseFlags(buffer)
    val value = buffer.readUnsignedByte()
    return value;
  }

  private fun parseCustomField(buffer: ParsableByteArray, size: Int) {
    // var nullBytes = buffer.readString(4)
    buffer.position += 4
    // val nameSpace = buffer.readString(size-4)
    buffer.position += size - 4
    val propertyNameWidth = buffer.readInt()
    // val nameBytes = buffer.readString(4)
    // nullBytes = buffer.readString(4)
    buffer.position += 8
    val propertyName = buffer.readString(propertyNameWidth - 12) // 4=nameWidth + 4=nameBytes + 4=nullBytes
    val propertyValueWidth = buffer.readInt()
    // val dataAtom = buffer.readString(4)
    buffer.position += 4
    parseFlags(buffer)
    val dataAtomSize = propertyValueWidth - 16; // buffer.readInt()
    val dataAtomValue = buffer.readString(dataAtomSize )

    when(propertyName) {
      "PART" -> part = dataAtomValue
      "SERIES" ->  series = dataAtomValue
    }
  }

  private fun parseDataAtomString(buffer: ParsableByteArray, size:Int): String? {
    parseFlags(buffer)
    val value = buffer.readString(size - 8/*, Charsets.ISO_8859_1*/)
    return value
  }


  private fun parseFlags(buffer: ParsableByteArray): Int {
    val byteBuffer = ByteBuffer.allocate(4)
    buffer.readBytes(byteBuffer, 4)
    val byteArray = byteBuffer.array()

    // val version = bytesToInt(byteArray, 0, 1)
    // 0=uint8
    // 1=text
    // 21=uint8
    val flags = bytesToInt(byteArray, 1, 3)
    // skip null space
    buffer.position += 4
    return flags
  }

  private fun bytesToInt(byteArray: ByteArray, offset: Int, length: Int): Int {
    val bytes = byteArray.drop(offset).take(length)
    var value = 0
    for (b in bytes) {
      value = (value shl 8) + (b.toInt() and 0xFF)
    }
    return value
  }

  private fun isAtomNameSupported(atomName: String): Boolean {
    return atomName.all { it -> it.isLetterOrDigit() || it == '-' || it == '©'}
  }
}

internal data class MetaAtom(val name:String, val position:Int, val size: Int) {
  val end: Int
    get() = position + size
  val children: MutableList<MetaAtom> = mutableListOf<MetaAtom>()
}
