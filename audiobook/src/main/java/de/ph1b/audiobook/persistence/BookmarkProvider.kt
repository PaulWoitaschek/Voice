package de.ph1b.audiobook.persistence

import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.persistence.internals.InternalBookmarkRegister
import rx.Observable
import v
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides access to bookmarks.
 *
 * @author: Paul Woitaschek
 */
@Singleton
class BookmarkProvider
@Inject constructor(private val register: InternalBookmarkRegister) {

    fun deleteBookmark(id: Long) = register.deleteBookmark(id)

    fun addBookmark(bookmark: Bookmark) = register.addBookmark(bookmark)

    fun addBookmarkAtBookPosition(book: Book, title: String) {
        val addedBookmark = Bookmark(book.currentChapter().file, title, book.time)
        addBookmark(addedBookmark)
        v { "Added bookmark=$addedBookmark" }
    }

    fun bookmarks(book: Book): Observable<List<Bookmark>> = Observable.fromCallable { register.bookmarks(book) }
}