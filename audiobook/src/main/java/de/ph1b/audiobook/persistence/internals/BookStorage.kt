package de.ph1b.audiobook.persistence.internals

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Chapter
import e
import java.io.File
import javax.inject.Inject


/**
 * Provides access to the peristent storage for bookmarks.
 *
 * @author: Paul Woitaschek
 */
class BookStorage
@Inject constructor(internalDb: InternalDb) {

  private val db by lazy { internalDb.writableDatabase }

  private fun books(active: Boolean) = db.asTransaction {
    return@asTransaction db.simpleQuery(table = BookTable.TABLE_NAME,
      columns = arrayOf(
        BookTable.ID,
        BookTable.NAME,
        BookTable.AUTHOR,
        BookTable.CURRENT_MEDIA_PATH,
        BookTable.PLAYBACK_SPEED,
        BookTable.ROOT,
        BookTable.TIME,
        BookTable.TYPE),
      selection = "${BookTable.ACTIVE} =?",
      selectionArgs = toStringArray(if (active) 1 else 0)
    ).mapRows {
      val bookId: Long = long(BookTable.ID)
      val bookName: String = string(BookTable.NAME)
      val bookAuthor: String? = stringNullable(BookTable.AUTHOR)
      var currentFile = File(string(BookTable.CURRENT_MEDIA_PATH))
      val bookSpeed: Float = float(BookTable.PLAYBACK_SPEED)
      val bookRoot: String = string(BookTable.ROOT)
      val bookTime: Int = int(BookTable.TIME)
      val bookType: String = string(BookTable.TYPE)

      val chapters = db.simpleQuery(table = ChapterTable.TABLE_NAME,
        columns = arrayOf(ChapterTable.NAME, ChapterTable.DURATION, ChapterTable.PATH),
        selection = "${ChapterTable.BOOK_ID} =?",
        selectionArgs = toStringArray(bookId))
        .mapRows {
          val name: String = string(ChapterTable.NAME)
          val duration: Int = int(ChapterTable.DURATION)
          val path: String = string(ChapterTable.PATH)
          Chapter(File(path), name, duration)
        }

      if (chapters.find { it.file == currentFile } == null) {
        e { "Couldn't get current file. Return first file" }
        currentFile = chapters[0].file
      }

      Book(bookId, Book.Type.valueOf(bookType), bookAuthor, currentFile, bookTime, bookName, chapters, bookSpeed, bookRoot)
    }
  }

  fun activeBooks() = books(true)

  fun orphanedBooks() = books(false)

  private fun setBookVisible(bookId: Long, visible: Boolean) = db.update(BookTable.TABLE_NAME,
    ContentValues().apply {
      put(BookTable.ACTIVE, if (visible) 1 else 0)
    }, "${BookTable.ID} =?", toStringArray(bookId))

  fun revealBook(bookId: Long) {
    setBookVisible(bookId, true)
  }

  fun hideBook(bookId: Long) {
    setBookVisible(bookId, false)
  }

  private fun SQLiteDatabase.insert(chapter: Chapter, bookId: Long) =
    insertOrThrow(ChapterTable.TABLE_NAME, null, chapter.toContentValues(bookId))

  private fun Chapter.toContentValues(bookId: Long) = ContentValues().apply {
    put(ChapterTable.DURATION, duration)
    put(ChapterTable.NAME, name)
    put(ChapterTable.PATH, file.absolutePath)
    put(ChapterTable.BOOK_ID, bookId)
  }

  private fun Book.toContentValues() = ContentValues().apply {
    put(BookTable.NAME, name)
    put(BookTable.AUTHOR, author)
    put(BookTable.ACTIVE, 1)
    put(BookTable.CURRENT_MEDIA_PATH, currentFile.absolutePath)
    put(BookTable.PLAYBACK_SPEED, playbackSpeed)
    put(BookTable.ROOT, root)
    put(BookTable.TIME, time)
    put(BookTable.TYPE, type.name)
  }

  fun updateBook(book: Book) = db.asTransaction {
    if (book.id == -1L) throw IllegalArgumentException("Book $book has an invalid id")

    // update book itself
    val bookCv = book.toContentValues()
    update(BookTable.TABLE_NAME, bookCv, "${BookTable.ID}=?", toStringArray(book.id))

    // delete old chapters and replace them with new ones
    delete(ChapterTable.TABLE_NAME, "${BookTable.ID}=?", toStringArray(book.id))
    book.chapters.forEach { insert(it, book.id) }
  }

  private fun toStringArray(vararg elements: Any) = Array(elements.size) {
    elements[it].toString()
  }

  fun addBook(toAdd: Book) = db.asTransaction {
    val bookCv = toAdd.toContentValues()
    val bookId = insertOrThrow(BookTable.TABLE_NAME, null, bookCv)
    val newBook = toAdd.copy(id = bookId)
    newBook.chapters.forEach { insert(it, bookId) }
    return@asTransaction newBook
  }
}