package de.ph1b.audiobook.persistence

import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.persistence.internals.SqlBookmarkStore
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides access to bookmarks.
 */
@Singleton
class BookmarkProvider
@Inject constructor(private val store: SqlBookmarkStore) {

  fun deleteBookmark(id: Long) = store.deleteBookmark(id)

  fun addBookmark(bookmark: Bookmark) = store.addBookmark(bookmark)

  fun addBookmarkAtBookPosition(book: Book, title: String): Bookmark {
    val addedBookmark = Bookmark(book.currentChapter().file, title, book.time)
    Timber.v("Added bookmark=$addedBookmark")
    return addBookmark(addedBookmark)
  }

  fun bookmarks(book: Book) = store.bookmarks(book)
}
