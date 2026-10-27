package voice.core.scanner

import dev.zacsweers.metro.Inject
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookMetadata
import voice.core.data.Chapter
import voice.core.data.CustomMetadata
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.CustomMetadataRepo
import voice.core.data.toUri
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.CachedDocumentFileFactory
import voice.core.logging.api.Logger
import java.time.Instant

@Inject
internal class BookParser(
  private val contentRepo: BookContentRepo,
  private val customMetadataRepo: CustomMetadataRepo,
  private val mediaAnalyzer: MediaAnalyzer,
  private val fileFactory: CachedDocumentFileFactory,
) {

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
      .copy(cover = stored.metadata.cover)
    val custom = customMetadataRepo.get(stored.id)
    val beforeNamesKeptApart = stored.metadata.fileLastModified == null
    val withNames = if (beforeNamesKeptApart) custom.withoutEarlierName(analyzed, file) else custom
    val updatedCustom = withNames.followingFiles(metadata)
    // before the content, as once the content is saved, the files count as read and this doesn't run again
    if (updatedCustom != custom) customMetadataRepo.put(updatedCustom)
    val updated = stored.copy(metadata = metadata)
    if (updated != stored) contentRepo.put(updated)
    return updated
  }

  // Before #3455, a book in a folder was named after its first file. Listeners rename books to the folder's name, so
  // that one stays.
  private fun CustomMetadata.withoutEarlierName(
    analyzed: Metadata?,
    file: CachedDocumentFile,
  ): CustomMetadata {
    if (file.isFile) return this
    val earlierNames = setOfNotNull(analyzed?.title, analyzed?.fileName)
    return copy(name = name?.takeUnless { it in earlierNames })
  }

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
          Logger.w("Document has no display name, using its URI as the book name")
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
  Book(content, chapters, CustomMetadata(content.id))
}
