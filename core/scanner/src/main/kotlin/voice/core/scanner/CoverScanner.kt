package voice.core.scanner

import android.content.Context
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.core.data.Book
import voice.core.data.isRemote
import voice.core.data.repo.BookRepository
import voice.core.data.toUri
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.CachedDocumentFileFactory
import voice.core.documentfile.nameWithoutExtension
import voice.core.logging.api.Logger
import java.io.File
import java.io.IOException

private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")

// Windows Media Player saves a small Folder.jpg next to the files
private val coverNames = listOf("cover", "front", "folder")

@Inject
internal class CoverScanner(
  private val context: Context,
  private val coverSaver: CoverSaver,
  private val coverExtractor: CoverExtractor,
  private val documentFileFactory: CachedDocumentFileFactory,
  private val bookRepository: BookRepository,
) {

  suspend fun scan() {
    coverSaver.moveExtractedCovers()
    // server books bring their covers along
    bookRepository.all().filterNot { it.id.isRemote }.forEach { findCoverForBook(it) }
  }

  private suspend fun findCoverForBook(book: Book) {
    val coverFile = book.content.cover
    if (coverFile != null && coverFile.exists()) {
      return
    }

    val image = coverSaver.newTemporaryFile()
    try {
      if (!findAndSaveCoverFromDisc(book, image)) {
        scanForEmbeddedCover(book, image)
      }
    } finally {
      image.delete()
    }
  }

  private suspend fun findAndSaveCoverFromDisc(
    book: Book,
    image: File,
  ): Boolean = withContext(Dispatchers.IO) {
    documentFileFactory.create(book.id.toUri()).children
      .filter { it.isImage() }
      .sortedBy { it.coverRank() }
      .forEach { child ->
        val copied = try {
          context.contentResolver.openInputStream(child.uri)?.use { input ->
            image.outputStream().use { output ->
              input.copyTo(output)
            }
          }
          true
        } catch (e: IOException) {
          Logger.w(e, "Can't copy a cover image")
          false
        } catch (e: IllegalStateException) {
          // On some Samsung Devices, openInputStream throws this exception, though it should not.
          Logger.w(e, "Can't copy a cover image")
          false
        }
        if (copied && coverSaver.saveExtractedCover(image, book.id)) {
          return@withContext true
        }
      }

    false
  }

  private fun CachedDocumentFile.coverRank(): Int {
    val rank = coverNames.indexOf(nameWithoutExtension().lowercase())
    return if (rank == -1) coverNames.size else rank
  }

  private fun CachedDocumentFile.isImage(): Boolean {
    if (!isFile) return false
    val name = name ?: return false
    return name.substringAfterLast(".", missingDelimiterValue = "").lowercase() in imageExtensions
  }

  private suspend fun scanForEmbeddedCover(
    book: Book,
    image: File,
  ) {
    book.chapters
      .take(5).forEach { chapter ->
        val success = coverExtractor.extractCover(
          input = chapter.id.toUri(),
          outputFile = image,
        )
        if (success && image.length() > 0 && coverSaver.saveExtractedCover(image, bookId = book.id)) {
          return
        }
      }
  }
}
