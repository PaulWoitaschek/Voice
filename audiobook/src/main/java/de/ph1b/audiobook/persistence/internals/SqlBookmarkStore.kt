package de.ph1b.audiobook.persistence.internals

import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Bookmark
import javax.inject.Inject

/**
 * Provides access to the peristent storage for bookmarks.
 *
 * @author Paul Woitaschek
 */
class SqlBookmarkStore
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
    val pathWhere = book.chapters.joinToString(separator = ",", prefix = "(", postfix = ")") { "?" }
    val pathArgs = book.chapters.map { it.file.absolutePath }

    val query = db.query(
        BookmarkTable.TABLE_NAME,
        listOf(BookmarkTable.PATH, BookmarkTable.TIME, BookmarkTable.TITLE, BookmarkTable.ID),
        "${BookmarkTable.PATH} IN $pathWhere",
        pathArgs)
    return query.mapRows { toBookmark() }
  }
}