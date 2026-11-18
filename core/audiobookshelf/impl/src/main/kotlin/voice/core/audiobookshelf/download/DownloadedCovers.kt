package voice.core.audiobookshelf.download

import android.content.Context
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import dev.zacsweers.metro.Inject
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.core.audiobookshelf.itemId
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.logging.api.Logger

private const val FILE_PREFIX = "abs-"

/** A downloaded book plays offline, so its cover must not depend on the cache. */
@Inject
internal class DownloadedCovers(
  private val bookRepository: BookRepository,
  private val context: Context,
) {

  suspend fun sync(
    books: List<Book>,
    downloaded: Set<BookId>,
  ) {
    val covers = mutableMapOf<BookId, File?>()
    books.forEach { book ->
      val current = book.content.metadata.cover
      // a cover a listener picked in 26.10.2 is in the same field and stays
      if (current != null && !current.name.startsWith(FILE_PREFIX)) return@forEach
      if (book.id in downloaded) {
        val file = File(File(context.filesDir, "bookCovers"), fileName(book))
        if (current?.name == file.name && file.exists()) return@forEach
        if (!load(book, file)) return@forEach
        if (current != null && current.name != file.name) current.delete()
        covers[book.id] = file
      } else if (current != null) {
        current.delete()
        covers[book.id] = null
      }
    }
    // a pause or a seek takes the same lock, so each book is read again inside it
    bookRepository.updateBooks(covers.keys) {
      it.copy(metadata = it.metadata.copy(cover = covers.getValue(it.id)))
    }
  }

  suspend fun keep(book: Book) {
    sync(listOf(book), downloaded = setOf(book.id))
  }

  private fun fileName(book: Book): String {
    val updatedAt = book.chapters.first().fileLastModified.toEpochMilliseconds()
    return "$FILE_PREFIX${book.id.itemId}-$updatedAt.jpg"
  }

  private suspend fun load(
    book: Book,
    file: File,
  ): Boolean {
    val cover = book.serverCoverUrl ?: return false
    val imageLoader = context.imageLoader
    val request =
      ImageRequest.Builder(context)
        .data(cover)
        .diskCacheKey(cover)
        .size(1)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .build()
    if (imageLoader.execute(request) !is SuccessResult) return false
    return withContext(Dispatchers.IO) {
      try {
        imageLoader.diskCache?.openSnapshot(cover)?.use { snapshot ->
          file.parentFile?.mkdirs()
          snapshot.data.toFile().copyTo(file, overwrite = true)
          true
        } ?: false
      } catch (e: IOException) {
        Logger.w(e, "Could not keep a cover")
        false
      }
    }
  }
}
