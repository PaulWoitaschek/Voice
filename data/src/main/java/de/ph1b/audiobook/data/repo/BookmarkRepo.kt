package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.repo.internals.IO
import de.ph1b.audiobook.data.repo.internals.SqlBookmarkStore
import kotlinx.coroutines.experimental.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides access to bookmarks.
 */
@Singleton
class BookmarkRepo
@Inject constructor(private val store: SqlBookmarkStore) {

  suspend fun deleteBookmark(id: Long) = withContext(IO) {
    store.deleteBookmark(id)
  }

  suspend fun addBookmark(bookmark: Bookmark) = withContext(IO) {
    store.addBookmark(bookmark)
  }

  suspend fun addBookmarkAtBookPosition(book: Book, title: String): Bookmark = withContext(IO) {
    val addedBookmark = Bookmark(book.currentChapter.file, title, book.positionInChapter)
    Timber.v("Added bookmark=$addedBookmark")
    addBookmark(addedBookmark)
  }

  suspend fun bookmarks(book: Book): List<Bookmark> = withContext(IO) {
    store.bookmarks(book)
  }
}
