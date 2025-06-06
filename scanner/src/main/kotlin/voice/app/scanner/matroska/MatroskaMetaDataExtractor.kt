package voice.app.scanner.matroska

import android.content.Context
import android.net.Uri
import org.ebml.EBMLReader
import org.ebml.Element
import org.ebml.MasterElement
import org.ebml.ProtoType
import org.ebml.StringElement
import org.ebml.UnsignedIntegerElement
import org.ebml.matroska.MatroskaDocTypes
import voice.logging.core.Logger
import java.util.Locale
import javax.inject.Inject

class MatroskaMetaDataExtractor
@Inject constructor(
  private val context: Context,
) {

  private lateinit var dataSource: SafSeekableDataSource
  private lateinit var reader: EBMLReader

  init {
    // Reference MatroskaDocTypes to force static init of its members which
    // register in static map used when identifying EBML Element types.
    MatroskaDocTypes.Void.level
  }

  fun readMediaInfo(uri: Uri): MatroskaMediaInfo {
    Logger.i("read media info $uri")
    init(uri)

    val firstElement = reader.readNextElement()
    if (!isValidEbmlHeader(firstElement)) {
      throw MatroskaParseException("Invalid ebml header")
    }

    var chapters = listOf<MatroskaChapter>()
    var album: String? = null
    var artist: String? = null
    var title: String? = null

    val segment = reader.readNextElement()
    if (segment isType MatroskaDocTypes.Segment) {
      segment.forEachChild { element ->
        when {
          element isType MatroskaDocTypes.Chapters -> {
            element.forEachChild {
              if (it isType MatroskaDocTypes.EditionEntry) {
                val (default, chaptersCandidate) = it.readEditionEntry()
                if (chaptersCandidate != null && (chapters.isEmpty() || default)) {
                  chapters = chaptersCandidate
                }
              }
            }
          }
          element isType MatroskaDocTypes.Info -> {
            // Read title from Info section
            element.forEachChild { infoChild ->
              if (infoChild isType MatroskaDocTypes.Title) {
                title = infoChild.readString()
              }
            }
          }
          element isType MatroskaDocTypes.Tags -> {
            val tagInfo = element.readTags()
            album = tagInfo.album ?: album
            artist = tagInfo.artist ?: artist
            title = tagInfo.title ?: title
          }
        }
      }
    } else {
      throw MatroskaParseException("Segment not the second element in the file: was ${segment.elementType.name} instead")
    }

    close()

    val preferredLanguages = listOf(Locale.getDefault().isO3Language, "eng")
    val flatChapters = MatroskaChapterFlattener.toChapters(chapters, preferredLanguages)

    return MatroskaMediaInfo(album, artist, title, flatChapters)
  }

  private fun Element.readTags(): TagInfo {
    var album: String? = null
    var artist: String? = null
    var title: String? = null

    forEachChild { tag ->
      if (tag isType MatroskaDocTypes.Tag) {
        val tagInfo = tag.readTag()
        album = tagInfo.album ?: album
        artist = tagInfo.artist ?: artist
        title = tagInfo.title ?: title
      }
    }

    return TagInfo(album, artist, title)
  }

  private fun Element.readTag(): TagInfo {
    var album: String? = null
    var artist: String? = null
    var title: String? = null

    forEachChild { simpleTag ->
      if (simpleTag isType MatroskaDocTypes.SimpleTag) {
        val (tagName, tagValue) = simpleTag.readSimpleTag()
        when (tagName?.uppercase()) {
          "ALBUM" -> album = tagValue
          "ARTIST", "PERFORMER" -> artist = tagValue
          "TITLE" -> title = tagValue
        }
      }
    }

    return TagInfo(album, artist, title)
  }

  private fun Element.readSimpleTag(): Pair<String?, String?> {
    var tagName: String? = null
    var tagString: String? = null

    forEachChild { child ->
      when {
        child isType MatroskaDocTypes.TagName -> {
          tagName = child.readString()
        }
        child isType MatroskaDocTypes.TagString -> {
          tagString = child.readString()
        }
      }
    }

    return Pair(tagName, tagString)
  }

  private fun isValidEbmlHeader(element: Element): Boolean =
    if (element isType MatroskaDocTypes.EBML) {
      element.forEachChild {
        if (it isType MatroskaDocTypes.DocType) {
          val docType = it.readString()
          if (docType != "matroska" && docType != "webm") {
            Logger.e("DocType $docType is not matroska")
            return false
          }
        }
      }
      true
    } else {
      Logger.e("EBML Header not the first element in the file")
      false
    }

  private fun init(uri: Uri) {
    dataSource = SafSeekableDataSource(context.contentResolver, uri)
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
    // https://www.matroska.org/technical/chapters.html
    var name: String? = null
    val languages = mutableSetOf<String>()
    forEachChild { child ->
      when {
        child isType MatroskaDocTypes.ChapString -> {
          name = child.readString()
        }
        child isType MatroskaDocTypes.ChapLanguage -> {
          languages.add(child.readString())
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
    forEachChild { child ->
      when {
        child isType MatroskaDocTypes.ChapterTimeStart -> {
          startTime = child.readUnsignedInteger()
        }
        child isType MatroskaDocTypes.ChapterAtom -> {
          // ignoring nested chapters
        }
        child isType MatroskaDocTypes.ChapterDisplay -> {
          names.add(child.readChapterDisplay())
        }
        child isType MatroskaDocTypes.ChapterFlagHidden -> {
          hidden = child.readUnsignedInteger() == 1L
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
            Logger.i("Ordered chapters. Ignoring affected edition")
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

private data class TagInfo(
  val album: String? = null,
  val artist: String? = null,
  val title: String? = null,
)

internal data class MatroskaChapter(
  val startTime: Long,
  private val names: List<MatroskaChapterName>,
  val children: List<MatroskaChapter>,
) {

  fun name(preferredLanguages: List<String> = emptyList()): String? {
    return preferredLanguages.firstNotNullOfOrNull { language ->
      names.find { language in it.languages }
        ?.name
    }
      ?: names.firstOrNull()?.name
  }
}

data class Chapter(val start: Long, val name: String)
internal class MatroskaParseException(message: String) : RuntimeException(message)

internal data class MatroskaChapterName(val name: String, val languages: Set<String>)

internal object MatroskaChapterFlattener {

  fun toChapters(list: List<MatroskaChapter>, preferredLanguages: List<String>): List<Chapter> {
    return list.mapIndexed { index, chapter ->
      Chapter(chapter.startTime / 1000000, chapter.name(preferredLanguages) ?: "Chapter ${index + 1}")
    }
  }
}
