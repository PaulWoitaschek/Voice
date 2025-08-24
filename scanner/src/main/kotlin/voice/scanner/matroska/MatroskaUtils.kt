package voice.scanner.matroska

import org.ebml.EBMLReader
import org.ebml.Element
import org.ebml.MasterElement
import org.ebml.ProtoType
import org.ebml.StringElement
import org.ebml.UnsignedIntegerElement
import org.ebml.io.DataSource
import org.ebml.matroska.MatroskaDocTypes

internal infix fun <T : Element> Element?.isType(t: ProtoType<T>) = this != null && isType(t.type)

internal inline fun Element.forEachChild(
  dataSource: DataSource,
  reader: EBMLReader,
  action: (Element) -> Unit,
) {
  if (this !is MasterElement) {
    throw MatroskaParseException("Expected a MasterElement")
  }
  var child = readNextChild(reader)
  while (child != null) {
    action(child)
    child.skipData(dataSource)
    child = readNextChild(reader)
  }
}

internal fun validateHeader(
  dataSource: DataSource,
  reader: EBMLReader,
) {
  val header = reader.readNextElement()
  if (!(header isType MatroskaDocTypes.EBML)) {
    throw MatroskaParseException("Invalid EBML header")
  }

  header.forEachChild(dataSource, reader) { element ->
    if (element isType MatroskaDocTypes.DocType) {
      val docType = element.readString(dataSource)
      if (docType !in listOf("matroska", "webm")) {
        throw MatroskaParseException("Unsupported doc type: $docType")
      }
    }
  }
}

internal fun Element.readString(dataSource: DataSource): String {
  if (this !is StringElement) {
    throw MatroskaParseException("Expected a StringElement")
  }
  readData(dataSource)
  return value
}

internal fun Element.readUnsignedInteger(dataSource: DataSource): Long {
  if (this !is UnsignedIntegerElement) {
    throw MatroskaParseException("Expected an UnsignedIntegerElement")
  }
  readData(dataSource)
  return value
}
