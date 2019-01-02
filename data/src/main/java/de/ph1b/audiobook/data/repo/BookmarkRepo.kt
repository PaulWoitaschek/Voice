package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.repo.internals.AppDb
import de.ph1b.audiobook.data.repo.internals.BookmarkDao
import de.ph1b.audiobook.data.repo.internals.transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides access to bookmarks.
 */
@Singleton
class BookmarkRepo
@Inject constructor(
  private val dao: BookmarkDao,
  private val appDb: AppDb
) {

  suspend fun deleteBookmark(id: Long) {
    withContext(Dispatchers.IO) {
      dao.deleteBookmark(id)
    }
  }

  suspend fun addBookmark(bookmark: Bookmark): Bookmark {
    return withContext(Dispatchers.IO) {
      val insertedId = dao.addBookmark(bookmark)
      bookmark.copy(id = insertedId)
    }
  }

  suspend fun addBookmarkAtBookPosition(book: Book, title: String): Bookmark {
    return withContext(Dispatchers.IO) {
      val addedBookmark =
        Bookmark(book.content.currentChapter.file, title, book.content.positionInChapter)
      Timber.v("Added bookmark=$addedBookmark")
      addBookmark(addedBookmark)
    }
  }

  suspend fun bookmarks(book: Book): List<Bookmark> {
    return withContext(Dispatchers.IO) {
      val files = book.content.chapters.map {
        it.file
      }
      // we can only query SQLITE_MAX_VARIABLE_NUMBER at once (999 bugs on some devices so we use a number a little smaller.)
      // if it's larger than the limit, we query in chunks.
      val limit = 990
      if (files.size > limit) {
        appDb.transaction {
          files.chunked(limit).flatMap {
            dao.allForFiles(it)
          }
        }
      } else {
        dao.allForFiles(files)
      }
    }
  }
}
