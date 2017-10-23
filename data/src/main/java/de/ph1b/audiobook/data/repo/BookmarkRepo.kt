package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.repo.internals.IO
import de.ph1b.audiobook.data.repo.internals.SqlBookmarkStore
import kotlinx.coroutines.experimental.run
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides access to bookmarks.
 */
@Singleton
class BookmarkRepo
@Inject constructor(private val store: SqlBookmarkStore) {

  suspend fun deleteBookmark(id: Long) = run(IO) {
    store.deleteBookmark(id)
  }

  suspend fun addBookmark(bookmark: Bookmark) = run(IO) {
    store.addBookmark(bookmark)
  }

  suspend fun addBookmarkAtBookPosition(book: Book, title: String): Bookmark = run(IO) {
    val addedBookmark = Bookmark(book.currentChapter.file, title, book.time)
    Timber.v("Added bookmark=$addedBookmark")
    addBookmark(addedBookmark)
  }

  suspend fun bookmarks(book: Book): List<Bookmark> = run(IO) {
    store.bookmarks(book)
  }
}
