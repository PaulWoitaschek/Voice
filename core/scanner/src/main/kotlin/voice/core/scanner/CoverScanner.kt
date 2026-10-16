package voice.core.scanner

import android.content.Context
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.core.data.Book
import voice.core.data.isRemote
import voice.core.data.toUri
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.CachedDocumentFileFactory
import voice.core.documentfile.nameWithoutExtension
import voice.core.logging.api.Logger
import java.io.IOException

private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
private val coverNames = setOf("cover", "folder", "front")

@Inject
internal class CoverScanner(
  private val context: Context,
  private val coverSaver: CoverSaver,
  private val coverExtractor: CoverExtractor,
  private val documentFileFactory: CachedDocumentFileFactory,
) {

  suspend fun scan(books: List<Book>) {
    // server books bring their covers along
    books.filterNot { it.id.isRemote }.forEach { findCoverForBook(it) }
  }

  private suspend fun findCoverForBook(book: Book) {
    val coverFile = book.content.cover
    if (coverFile != null && coverFile.exists()) {
      return
    }

    val foundOnDisc = findAndSaveCoverFromDisc(book)
    if (foundOnDisc) {
      return
    }

    scanForEmbeddedCover(book)
  }

  private suspend fun findAndSaveCoverFromDisc(book: Book): Boolean = withContext(Dispatchers.IO) {
    documentFileFactory.create(book.id.toUri()).children
      .filter { it.isImage() }
      .sortedByDescending { it.nameWithoutExtension().lowercase() in coverNames }
      .forEach { child ->
        val coverFile = coverSaver.newBookCoverFile()
        val worked = try {
          context.contentResolver.openInputStream(child.uri)?.use { input ->
            coverFile.outputStream().use { output ->
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
        if (worked) {
          coverSaver.setCoverFromFiles(coverFile, book.id)
          return@withContext true
        }
      }

    false
  }

  private fun CachedDocumentFile.isImage(): Boolean {
    if (!isFile) return false
    val name = name ?: return false
    return name.substringAfterLast(".", missingDelimiterValue = "").lowercase() in imageExtensions
  }

  private suspend fun scanForEmbeddedCover(book: Book) {
    val coverFile = coverSaver.newBookCoverFile()
    book.chapters
      .take(5).forEach { chapter ->
        val success = coverExtractor.extractCover(
          input = chapter.id.toUri(),
          outputFile = coverFile,
        )
        if (success && coverFile.exists() && coverFile.length() > 0) {
          coverSaver.setCoverFromFiles(coverFile, bookId = book.id)
          return
        }
      }
  }
}
