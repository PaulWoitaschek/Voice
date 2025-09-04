package voice.core.scanner

import android.net.Uri
import dev.zacsweers.metro.Inject
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.getOrPut
import voice.core.data.toUri
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.CachedDocumentFileFactory
import voice.core.logging.core.Logger
import java.time.Instant

@Inject
internal class BookParser(
  private val contentRepo: BookContentRepo,
  private val mediaAnalyzer: MediaAnalyzer,
  private val fileFactory: CachedDocumentFileFactory,
) {

  suspend fun parseAndStore(
    chapters: List<Chapter>,
    file: CachedDocumentFile,
  ): BookContent {
    val id = BookId(file.uri)
    return contentRepo.getOrPut(id) {
      val uri = chapters.first().id.toUri()
      val analyzed = mediaAnalyzer.analyze(fileFactory.create(uri))
      BookContent(
        id = id,
        isActive = true,
        addedAt = Instant.now(),
        author = analyzed?.artist,
        lastPlayedAt = Instant.EPOCH,
        name = analyzed?.album
          ?: analyzed?.title
          ?: file.bookName(),
        playbackSpeed = 1F,
        skipSilence = false,
        chapters = chapters.map { it.id },
        positionInChapter = 0L,
        currentChapter = chapters.first().id,
        cover = null,
        gain = 0F,
        genre = analyzed?.genre,
        narrator = analyzed?.narrator,
        series = analyzed?.series,
        part = analyzed?.part,
      ).also {
        validateIntegrity(it, chapters)
      }
    }
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
  Book(content, chapters)
}

internal fun Uri.filePath(): String? {
  return pathSegments.lastOrNull()
    ?.dropWhile { it != ':' }
    ?.removePrefix(":")
}
