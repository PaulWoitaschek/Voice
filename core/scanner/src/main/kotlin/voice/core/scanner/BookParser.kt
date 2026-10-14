package voice.core.scanner

import dev.zacsweers.metro.Inject
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookMetadata
import voice.core.data.Chapter
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.getOrPut
import voice.core.data.toUri
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.CachedDocumentFileFactory
import voice.core.logging.api.Logger
import java.time.Instant

@Inject
internal class BookParser(
  private val contentRepo: BookContentRepo,
  private val mediaAnalyzer: MediaAnalyzer,
  private val fileFactory: CachedDocumentFileFactory,
) {

  /**
   * The book the [chapters] make up, with what its files say now. A new book is stored right away, and so is a book
   * whose files say something else now.
   */
  suspend fun parseAndStore(
    chapters: List<Chapter>,
    file: CachedDocumentFile,
    firstChapterMetadata: Metadata?,
  ): BookContent {
    val stored = contentRepo.get(BookId(file.uri))
      ?: return parse(chapters, file, firstChapterMetadata).also { contentRepo.put(it) }
    val firstChapter = chapters.first()
    val upToDate = firstChapterMetadata == null &&
      stored.chapters.first() == firstChapter.id &&
      stored.metadata.fileLastModified == firstChapter.fileLastModified
    if (upToDate) return stored
    val analyzed = firstChapterMetadata ?: analyze(firstChapter)
    val metadata = metadata(chapters, file, analyzed)
      // a cover is only looked for when the book has none
      .copy(cover = stored.metadata.cover)
    val beforeNamesKeptApart = stored.metadata.fileLastModified == null
    val withNames = if (beforeNamesKeptApart) stored.withoutEarlierName(analyzed, file) else stored
    return withNames.withMetadata(metadata).also {
      if (it != stored) contentRepo.put(it)
    }
  }

  // Earlier versions named a book in a folder after the title of its first file, or the file's name without one, so
  // such a name isn't one the listener gave. A name like the folder's stays, as listeners rename books to it.
  private fun BookContent.withoutEarlierName(
    analyzed: Metadata?,
    file: CachedDocumentFile,
  ): BookContent {
    if (file.isFile) return this
    val earlierNames = setOfNotNull(analyzed?.title, analyzed?.fileName)
    return copy(customName = customName?.takeUnless { it in earlierNames })
  }

  /**
   * The book the [chapters] make up, without storing it. Unless the [firstChapterMetadata] is passed,
   * the first chapter is analyzed for it.
   */
  suspend fun parse(
    chapters: List<Chapter>,
    file: CachedDocumentFile,
    firstChapterMetadata: Metadata?,
  ): BookContent {
    return parse(chapters, BookId(file.uri), firstChapterMetadata ?: analyze(chapters.first()), file)
  }

  fun parse(
    chapters: List<Chapter>,
    id: BookId,
    analyzed: Metadata?,
    file: CachedDocumentFile,
  ): BookContent {
    return BookContent(
      id = id,
      isActive = true,
      addedAt = Instant.now(),
      lastPlayedAt = Instant.EPOCH,
      playbackSpeed = 1F,
      skipSilence = false,
      chapters = chapters.map { it.id },
      positionInChapter = 0L,
      currentChapter = chapters.first().id,
      gain = 0F,
      metadata = metadata(chapters, file, analyzed),
      customName = null,
      customCover = null,
    ).also {
      validateIntegrity(it, chapters)
    }
  }

  private suspend fun analyze(chapter: Chapter): Metadata? {
    return mediaAnalyzer.analyze(fileFactory.create(chapter.id.toUri()))
  }

  private fun metadata(
    chapters: List<Chapter>,
    file: CachedDocumentFile,
    analyzed: Metadata?,
  ): BookMetadata {
    return BookMetadata(
      name = analyzed?.album
        ?: (analyzed?.title ?: analyzed?.fileName)?.takeIf { file.isFile }
        ?: file.bookName(),
      author = analyzed?.artist,
      album = analyzed?.album,
      title = analyzed?.title,
      genre = analyzed?.genre,
      narrator = analyzed?.narrator,
      series = analyzed?.series,
      part = analyzed?.part,
      cover = null,
      fileLastModified = chapters.first().fileLastModified,
    )
  }

  private fun CachedDocumentFile.bookName(): String {
    val fileName = name
    return if (fileName == null) {
      uri.toString()
        .removePrefix("/storage/emulated/0/")
        .removePrefix("/storage/emulated/")
        .removePrefix("/storage/")
        .also {
          Logger.w("Could not parse fileName from $this. Fallback to $it")
        }
    } else {
      if (isFile) {
        fileName.substringBeforeLast(".")
      } else {
        fileName
      }
    }
  }
}

internal fun validateIntegrity(
  content: BookContent,
  chapters: List<Chapter>,
) {
  // the init block performs integrity validation
  @Suppress("RETURN_VALUE_NOT_USED")
  Book(content, chapters)
}
