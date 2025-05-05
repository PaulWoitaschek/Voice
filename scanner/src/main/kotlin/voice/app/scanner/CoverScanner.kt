package voice.app.scanner

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.data.Book
import voice.data.toUri
import voice.logging.core.Logger
import java.io.IOException
import javax.inject.Inject

class CoverScanner
@Inject constructor(
  private val context: Context,
  private val coverSaver: CoverSaver,
  private val coverExtractor: CoverExtractor,
) {

  suspend fun scan(books: List<Book>) {
    books.forEach { findCoverForBook(it) }
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
    val documentFile = try {
      DocumentFile.fromTreeUri(context, book.id.toUri())
    } catch (_: IllegalArgumentException) {
      null
    } ?: return@withContext false

    if (!documentFile.isDirectory) {
      return@withContext false
    }

    documentFile.listFiles().forEach { child ->
      if (child.isFile && child.canRead() && child.type?.startsWith("image/") == true) {
        val coverFile = coverSaver.newBookCoverFile()
        val worked = try {
          context.contentResolver.openInputStream(child.uri)?.use { input ->
            coverFile.outputStream().use { output ->
              input.copyTo(output)
            }
          }
          true
        } catch (e: IOException) {
          Logger.w(e, "Error while copying the cover from ${child.uri}")
          false
        } catch (e: IllegalStateException) {
          // On some Samsung Devices, openInputStream throws this exception, though it should not.
          Logger.w(e, "Error while copying the cover from ${child.uri}")
          false
        }
        if (worked) {
          coverSaver.setBookCover(coverFile, book.id)
          return@withContext true
        }
      }
    }

    false
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
          coverSaver.setBookCover(coverFile, bookId = book.id)
          return
        }
      }
  }
}
