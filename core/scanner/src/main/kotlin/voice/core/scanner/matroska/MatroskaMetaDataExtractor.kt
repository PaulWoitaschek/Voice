package voice.core.scanner.matroska

import android.content.Context
import android.net.Uri
import dev.zacsweers.metro.Inject
import org.ebml.EBMLReader
import org.ebml.Element
import org.ebml.matroska.MatroskaDocTypes
import voice.core.data.MarkData
import java.util.Locale

internal class MatroskaMetaDataExtractor(
  private val dataSource: SafSeekableDataSource,
  private val reader: EBMLReader,
) : AutoCloseable {

  @Inject
  class Factory(private val context: Context) {
    fun create(uri: Uri): MatroskaMetaDataExtractor {
      val dataSource = SafSeekableDataSource(context.contentResolver, uri)
      val reader = EBMLReader(dataSource)
      return MatroskaMetaDataExtractor(dataSource, reader)
    }
  }

  init {
    // Force static initialization
    MatroskaDocTypes.Void.level
  }

  fun readMediaInfo(): MatroskaMediaInfo = try {
    validateHeader(dataSource, reader)

    val segment = reader.readNextElement()
    if (!(segment isType MatroskaDocTypes.Segment)) {
      throw MatroskaParseException("Expected Segment element")
    }

    var chapters = emptyList<MatroskaChapter>()
    var album: String? = null
    var artist: String? = null
    var title: String? = null

    segment.forEachChild { element ->
      when {
        element isType MatroskaDocTypes.Chapters -> {
          chapters = readChapters(element)
        }
        element isType MatroskaDocTypes.Info -> {
          title = readTitle(element) ?: title
        }
        element isType MatroskaDocTypes.Tags -> {
          val tags = readTags(element)
          album = tags.album ?: album
          artist = tags.artist ?: artist
          title = tags.title ?: title
        }
      }
    }

    val preferredLanguages = listOf(Locale.getDefault().isO3Language, "eng")
    MatroskaMediaInfo(
      album = album,
      artist = artist,
      title = title,
      chapters = chapters.mapIndexed { index, chapter ->
        MarkData(
          chapter.startTime / 1000000,
          chapter.bestName(preferredLanguages) ?: "Chapter ${index + 1}",
        )
      },
    )
  } catch (e: RuntimeException) {
    // jebml throws undeclared exceptions, so we need wrap them :/
    throw MatroskaParseException("Failed to read Matroska metadata: ${e.message}", e)
  }

  private fun readChapters(element: Element): List<MatroskaChapter> {
    val chapters = mutableListOf<MatroskaChapter>()

    element.forEachChild { child ->
      if (child isType MatroskaDocTypes.EditionEntry) {
        chapters += readEdition(child)
      }
    }

    return chapters
  }

  private fun readEdition(element: Element): List<MatroskaChapter> {
    val chapters = mutableListOf<MatroskaChapter>()
    var hidden = false

    element.forEachChild { child ->
      when {
        child isType MatroskaDocTypes.ChapterAtom -> {
          readChapter(child)?.let { chapters.add(it) }
        }
        child isType MatroskaDocTypes.EditionFlagHidden -> {
          hidden = child.readUnsignedInteger(dataSource) == 1L
        }
        child isType MatroskaDocTypes.EditionFlagOrdered -> {
          if (child.readUnsignedInteger(dataSource) == 1L) {
            hidden = true // Skip ordered chapters
          }
        }
      }
    }

    return if (hidden) emptyList() else chapters
  }

  private fun readChapter(element: Element): MatroskaChapter? {
    var startTime: Long = -1
    val names = mutableListOf<MatroskaChapterName>()
    var hidden = false

    element.forEachChild { child ->
      when {
        child isType MatroskaDocTypes.ChapterTimeStart -> {
          startTime = child.readUnsignedInteger(dataSource)
        }
        child isType MatroskaDocTypes.ChapterDisplay -> {
          names.add(readChapterName(child))
        }
        child isType MatroskaDocTypes.ChapterFlagHidden -> {
          hidden = child.readUnsignedInteger(dataSource) == 1L
        }
      }
    }

    if (hidden || startTime == -1L) return null

    return MatroskaChapter(startTime, names)
  }

  private fun readChapterName(element: Element): MatroskaChapterName {
    var name: String? = null
    val languages = mutableSetOf<String>()

    element.forEachChild { child ->
      when {
        child isType MatroskaDocTypes.ChapString -> {
          name = child.readString()
        }
        child isType MatroskaDocTypes.ChapLanguage -> {
          languages.add(child.readString())
        }
      }
    }

    return MatroskaChapterName(name = name ?: "", languages = languages)
  }

  private fun readTitle(element: Element): String? {
    element.forEachChild { child ->
      if (child isType MatroskaDocTypes.Title) {
        return child.readString()
      }
    }
    return null
  }

  private fun readTags(element: Element): TagInfo {
    var album: String? = null
    var artist: String? = null
    var title: String? = null

    element.forEachChild { tag ->
      if (tag isType MatroskaDocTypes.Tag) {
        tag.forEachChild { simpleTag ->
          if (simpleTag isType MatroskaDocTypes.SimpleTag) {
            val (name, value) = readSimpleTag(simpleTag)
            when (name?.uppercase()) {
              "ALBUM" -> album = value
              "ARTIST", "PERFORMER" -> artist = value
              "TITLE" -> title = value
            }
          }
        }
      }
    }

    return TagInfo(album = album, artist = artist, title = title)
  }

  private fun readSimpleTag(element: Element): Pair<String?, String?> {
    var tagName: String? = null
    var tagValue: String? = null

    element.forEachChild { child ->
      when {
        child isType MatroskaDocTypes.TagName -> tagName = child.readString()
        child isType MatroskaDocTypes.TagString -> tagValue = child.readString()
      }
    }

    return tagName to tagValue
  }

  private inline fun Element.forEachChild(action: (Element) -> Unit) = forEachChild(dataSource, reader, action)
  private fun Element.readString(): String = readString(dataSource)

  override fun close() = dataSource.close()
}

private data class TagInfo(
  val album: String? = null,
  val artist: String? = null,
  val title: String? = null,
)

internal class MatroskaParseException : RuntimeException {
  constructor(message: String) : super(message)
  constructor(message: String, cause: Throwable) : super(message, cause)
}
