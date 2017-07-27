package de.ph1b.audiobook.chapterreader.matroska

import dagger.Reusable
import de.ph1b.audiobook.common.Logger
import org.ebml.EBMLReader
import org.ebml.Element
import org.ebml.MasterElement
import org.ebml.ProtoType
import org.ebml.StringElement
import org.ebml.UnsignedIntegerElement
import org.ebml.io.FileDataSource
import org.ebml.matroska.MatroskaDocTypes
import java.io.File
import javax.inject.Inject


@Reusable class ReadAsMatroskaChapters @Inject constructor(private val logger: Logger) {

  private lateinit var dataSource: FileDataSource
  private lateinit var reader: EBMLReader

  init {
    // Reference MatroskaDocTypes to force static init of its members which
    // register in static map used when identifying EBML Element types.
    MatroskaDocTypes.Void.level
  }

  @Synchronized
  fun read(file: File): List<MatroskaChapter> {
    logger.i("read $file")
    init(file)

    val firstElement = reader.readNextElement()
    if (!isValidEbmlHeader(firstElement)) {
      throw MatroskaParseException("Invalid ebml header")
    }

    var chapters = listOf<MatroskaChapter>()
    val segment = reader.readNextElement()
    if (segment isType MatroskaDocTypes.Segment) {
      segment.forEachChild {
        if (it isType MatroskaDocTypes.Chapters) {
          it.forEachChild {
            if (it isType MatroskaDocTypes.EditionEntry) {
              val (default, chaptersCandidate) = it.readEditionEntry()
              if (chaptersCandidate != null && (chapters.isEmpty() || default)) {
                chapters = chaptersCandidate
              }
            }
          }
        }
      }
    } else {
      throw MatroskaParseException("Segment not the second element in the file: was ${segment.elementType.name} instead")
    }

    close()

    return chapters
  }

  private fun isValidEbmlHeader(element: Element): Boolean {
    if (element isType MatroskaDocTypes.EBML) {
      element.forEachChild {
        if (it isType MatroskaDocTypes.DocType) {
          val docType = it.readString()
          if (docType != "matroska" && docType != "webm") {
            logger.e("DocType $docType is not de.ph1b.audiobook.chapterreader.de.ph1b.audiobook.chapterreader.matroska")
            return false
          }
        }
      }
      return true
    } else {
      logger.e("EBML Header not the first element in the file")
      return false
    }
  }

  private fun init(file: File) {
    dataSource = FileDataSource(file.path)
    reader = EBMLReader(dataSource)
  }

  private inline fun Element.forEachChild(action: (Element) -> Unit) {
    this as MasterElement
    var child = readNextChild(reader)
    while (child != null) {
      action(child)
      // Calling skipData is a nop after calling readData/skipData.
      child.skipData(dataSource)
      child = readNextChild(reader)
    }
  }

  private fun Element.readString(): String {
    this as StringElement
    readData(dataSource)
    return value
  }

  private fun Element.readUnsignedInteger(): Long {
    this as UnsignedIntegerElement
    readData(dataSource)
    return value
  }

  private fun Element.readChapterDisplay(): MatroskaChapterName {
    var name: String? = null
    val languages = mutableSetOf<String>()
    forEachChild {
      when {
        it isType MatroskaDocTypes.ChapString -> {
          name = it.readString()
        }
        it isType MatroskaDocTypes.ChapLanguage -> {
          languages.add(it.readString())
        }
      }
    }
    if (name == null) {
      throw MatroskaParseException("Missing mandatory ChapterString in ChapterDisplay")
    }
    return MatroskaChapterName(name!!, languages)
  }

  private fun Element.readChapterAtom(): MatroskaChapter? {
    var startTime: Long? = null
    val names = mutableListOf<MatroskaChapterName>()
    val children = mutableListOf<MatroskaChapter>()
    var hidden = false
    forEachChild {
      when {
        it isType MatroskaDocTypes.ChapterTimeStart -> {
          startTime = it.readUnsignedInteger()
        }
        it isType MatroskaDocTypes.ChapterAtom -> {
          val chapter = it.readChapterAtom()
          if (chapter != null) children.add(chapter)
        }
        it isType MatroskaDocTypes.ChapterDisplay -> {
          names.add(it.readChapterDisplay())
        }
        it isType MatroskaDocTypes.ChapterFlagHidden -> {
          hidden = it.readUnsignedInteger() == 1L
        }
      }
    }
    if (hidden) return null
    if (startTime == null) {
      throw MatroskaParseException("Missing mandatory ChapterTimeStart element in ChapterAtom")
    }
    return MatroskaChapter(startTime!!, names, children)
  }

  private fun Element.readEditionEntry(): Pair<Boolean, List<MatroskaChapter>?> {
    val chapters = mutableListOf<MatroskaChapter>()
    var hidden = false
    var default = false
    forEachChild {
      when {
        it isType MatroskaDocTypes.ChapterAtom -> {
          val chapter = it.readChapterAtom()
          if (chapter != null) chapters.add(chapter)
        }
        it isType MatroskaDocTypes.EditionFlagHidden -> {
          hidden = it.readUnsignedInteger() == 1L
        }
        it isType MatroskaDocTypes.EditionFlagOrdered -> {
          if (it.readUnsignedInteger() == 1L) {
            // Ordered chapters support is problematic,
            // it varies across players so lets ignore them.
            hidden = true
            logger.i("Ordered chapters. Ignoring affected edition")
          }
        }
        it isType MatroskaDocTypes.EditionFlagDefault -> {
          default = it.readUnsignedInteger() == 1L
        }
      }
    }
    if (hidden) return false to null
    return default to chapters
  }

  private fun close() {
    dataSource.close()
  }

  private infix fun <T : Element> Element?.isType(t: ProtoType<T>) = this != null && isType(t.type)
}
