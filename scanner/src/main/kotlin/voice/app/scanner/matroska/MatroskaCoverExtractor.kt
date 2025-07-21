package voice.app.scanner.matroska

import android.content.Context
import android.net.Uri
import dev.zacsweers.metro.Inject
import org.ebml.EBMLReader
import org.ebml.Element
import org.ebml.matroska.MatroskaDocTypes
import voice.logging.core.Logger
import java.io.File

@Inject
class MatroskaCoverExtractor(private val context: Context) {

  init {
    // force static initialization of MatroskaDocTypes
    MatroskaDocTypes.Void.level
  }

  private data class AttachmentInfo(
    val filename: String?,
    val mimeType: String?,
    val description: String?,
    val data: ByteArray,
    val priority: Int,
  )

  fun extract(
    input: Uri,
    outputFile: File,
  ): Boolean {
    return try {
      SafSeekableDataSource(context.contentResolver, input).use { dataSource ->
        val reader = EBMLReader(dataSource)

        validateHeader(dataSource, reader)

        val segment = reader.readNextElement()
        if (!(segment isType MatroskaDocTypes.Segment)) {
          throw MatroskaParseException("Expected Segment element")
        }

        val attachments = mutableListOf<AttachmentInfo>()

        segment.forEachChild(dataSource, reader) { child ->
          if (child.isType(MatroskaDocTypes.Attachments)) {
            Logger.d("Found Attachments segment")
            child.forEachChild(dataSource, reader) { attachmentChild ->
              if (attachmentChild.isType(MatroskaDocTypes.AttachedFile)) {
                val attachment = processAttachment(dataSource, reader, attachmentChild)
                if (attachment != null) {
                  attachments.add(attachment)
                }
              }
            }
          }
        }

        val bestCover = findBestCover(attachments)
        if (bestCover != null) {
          outputFile.writeBytes(bestCover.data)
          Logger.d("Extracted cover: ${bestCover.filename} (priority: ${bestCover.priority})")
          return true
        }

        Logger.d("No cover art found in Matroska attachments")
        false
      }
    } catch (e: RuntimeException) {
      // jebml throws undeclared exceptions, so we need to wrap them
      Logger.e(e, "Error extracting cover from Matroska file: $input")
      return false
    }
  }

  private fun processAttachment(
    dataSource: SafSeekableDataSource,
    reader: EBMLReader,
    attachedFile: Element,
  ): AttachmentInfo? {
    var filename: String? = null
    var mimeType: String? = null
    var description: String? = null
    var data: ByteArray? = null

    attachedFile.forEachChild(dataSource, reader) { child ->
      when {
        child.isType(MatroskaDocTypes.FileName.type) -> {
          filename = child.readString(dataSource)
          Logger.d("Found filename: $filename")
        }
        child.isType(MatroskaDocTypes.FileMimeType.type) -> {
          mimeType = child.readString(dataSource)
          Logger.d("Found mimetype: $mimeType")
        }
        child.isType(MatroskaDocTypes.FileDescription.type) -> {
          description = child.readString(dataSource)
          Logger.d("Found description: $description")
        }
        child.isType(MatroskaDocTypes.FileData) -> {
          child.readData(dataSource)
          data = child.dataArray
          Logger.d("Found file data: ${data?.size} bytes")
        }
      }
    }

    return if (data != null && isImageMimeType(mimeType)) {
      val priority = calculatePriority(filename = filename, description = description)
      AttachmentInfo(filename = filename, mimeType = mimeType, description = description, data = data!!, priority = priority)
    } else {
      null
    }
  }

  private fun findBestCover(attachments: List<AttachmentInfo>): AttachmentInfo? {
    return attachments.maxByOrNull { calculatePriority(filename = it.filename, description = it.description) }
  }

  private fun isImageMimeType(mimeType: String?): Boolean {
    return mimeType?.startsWith("image/") == true
  }

  private fun calculatePriority(
    filename: String?,
    description: String?,
  ): Int {
    val elements = setOfNotNull(filename, description)
    fun containsElement(element: String): Boolean {
      return elements.any { it.contains(element, ignoreCase = true) }
    }
    return when {
      containsElement("front") -> 10
      containsElement("cover") -> 9
      containsElement("folder") -> 8
      containsElement("album") -> 8
      containsElement("artwork") -> 6
      containsElement("thumb") -> 6
      filename != null -> 2
      else -> 1
    }
  }
}
