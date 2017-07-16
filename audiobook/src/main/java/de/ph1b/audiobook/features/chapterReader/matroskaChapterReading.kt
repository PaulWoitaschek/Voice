package de.ph1b.audiobook.features.chapterReader

import i
import org.ebml.*
import org.ebml.io.FileDataSource
import org.ebml.matroska.MatroskaDocTypes
import java.io.File
import java.lang.RuntimeException

internal fun readMatroskaChapters(file: File): List<MatroskaChapter> {
  // Reference MatroskaDocTypes to force static init of its members which
  // register in static map used when identifying EBML Element types.
  MatroskaDocTypes.Void.level

  val dataSource = FileDataSource(file.path)
  val reader = EBMLReader(dataSource)

  fun Element.forEachChild(f: (Element) -> Unit) {
    this as MasterElement
    var child = readNextChild(reader)
    while (child != null) {
      f(child)
      // Calling skipData is a nop after calling readData/skipData.
      child.skipData(dataSource)
      child = readNextChild(reader)
    }
  }

  fun Element.readString(): String {
    this as StringElement
    readData(dataSource)
    return value
  }

  fun Element.readUnsignedInteger(): Long {
    this as UnsignedIntegerElement
    readData(dataSource)
    return value
  }

  fun Element.readChapterDisplay(): MatroskaChapterName {
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

  fun Element.readChapterAtom(): MatroskaChapter? {
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

  fun Element.readEditionEntry(): Pair<Boolean, List<MatroskaChapter>?> {
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
            i { "Ordered chapters in file \"${file.path}\", ignoring affected edition" }
          }
        }
        it isType MatroskaDocTypes.EditionFlagDefault -> {
          default = it.readUnsignedInteger() == 1L
        }
      }
    }
    if (hidden) return Pair(false, null)
    return Pair(default, chapters)
  }

  val ebmlHeader = reader.readNextElement()
  if (ebmlHeader isType MatroskaDocTypes.EBML) {
    ebmlHeader.forEachChild {
      if (it isType MatroskaDocTypes.DocType) {
        val docType = it.readString()
        if (docType != "matroska" && docType != "webm") {
          throw MatroskaParseException("DocType is not matroska, \"$docType\"")
        }
      }
    }
  } else {
    throw MatroskaParseException("EBML Header not the first element in the file")
  }

  var chapters = listOf<MatroskaChapter>()
  val segment = reader.readNextElement()
  if (segment isType MatroskaDocTypes.Segment) {
    segment.forEachChild {
      if (it isType MatroskaDocTypes.Chapters) {
        it.forEachChild {
          if (it isType MatroskaDocTypes.EditionEntry) {
            val (default, chaptersCandidate) = it.readEditionEntry()
            if (chaptersCandidate != null && (chapters.isEmpty() || default))
              chapters = chaptersCandidate
          }
        }
      }
    }
  } else {
    throw MatroskaParseException(
        "Segment not the second element in the file: was ${segment.elementType.name} instead")
  }

  return chapters
}

class MatroskaParseException(message: String): RuntimeException(message)

private infix fun <T : Element> Element?.isType(t: ProtoType<T>) = this != null && isType(t.type)

internal data class MatroskaChapterName(val name: String, val languages: Set<String>)

internal data class MatroskaChapter(
    val startTime: Long,
    val names: List<MatroskaChapterName>,
    val children: List<MatroskaChapter>) {
  fun getName(vararg preferredLanguages: String): String?
      = preferredLanguages
      .map { language ->
          names.find { language in it.languages }?.name
      }
      .filterNotNull()
      .firstOrNull() ?: names.firstOrNull()?.name
}
