package de.ph1b.audiobook.persistence.internals

import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Bookmark
import java.util.*
import javax.inject.Inject

/**
 * Provides access to the peristent storage for bookmarks.
 *
 * @author: Paul Woitaschek
 */
class InternalBookmarkRegister
@Inject constructor(internalDb: InternalDb) {

  private val db by lazy { internalDb.writableDatabase }

  fun deleteBookmark(id: Long) {
    db.delete(BookmarkTable.TABLE_NAME, "${BookmarkTable.ID} =?", arrayOf(id.toString()))
  }

  fun addBookmark(bookmark: Bookmark): Bookmark {
    val cv = bookmark.toContentValues()
    val insertedId = db.insertOrThrow(BookmarkTable.TABLE_NAME, null, cv)
    return bookmark.copy(id = insertedId)
  }

  fun bookmarks(book: Book): List<Bookmark> {
    val whereBuilder = StringBuilder()
    val pathAlike = "${BookmarkTable.PATH} =?"
    for (i in 0..book.chapters.size - 1) {
      if (i > 0) whereBuilder.append(" OR ")
      whereBuilder.append(pathAlike)
    }
    val cursor = db.query(BookmarkTable.TABLE_NAME,
      arrayOf(BookmarkTable.PATH, BookmarkTable.TIME, BookmarkTable.TITLE, BookmarkTable.ID),
      whereBuilder.toString(),
      book.chapters.map { it.file.absolutePath }.toTypedArray(),
      null, null, null)

    val bookmarks = ArrayList<Bookmark>(cursor.count)
    cursor.moveToNextLoop {
      bookmarks.add(cursor.toBookmark())
    }
    return bookmarks
  }
}