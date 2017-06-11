package de.ph1b.audiobook.persistence.internals

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.squareup.moshi.Moshi
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Chapter
import de.ph1b.audiobook.misc.SparseArrayAdapter
import de.ph1b.audiobook.misc.emptySparseArray
import e
import java.io.File
import javax.inject.Inject


/**
 * Provides access to the persistent storage for bookmarks.
 *
 * @author Paul Woitaschek
 */
class BookStorage
@Inject constructor(
    internalDb: InternalDb,
    moshi: Moshi
) {

  private val chapterMarkAdapter = SparseArrayAdapter<String>(moshi.adapter(String::class.java))

  private val db by lazy { internalDb.writableDatabase }

  private fun books(active: Boolean) = db.asTransaction {
    return@asTransaction db.query(table = BookTable.TABLE_NAME,
        columns = listOf(
            BookTable.ID,
            BookTable.NAME,
            BookTable.AUTHOR,
            BookTable.CURRENT_MEDIA_PATH,
            BookTable.PLAYBACK_SPEED,
            BookTable.ROOT,
            BookTable.TIME,
            BookTable.TYPE,
            BookTable.LOUDNESS_GAIN),
        selection = "${BookTable.ACTIVE} =?",
        selectionArgs = listOf(if (active) 1 else 0)
    ).mapRows {
      val bookId: Long = long(BookTable.ID)
      val bookName: String = string(BookTable.NAME)
      val bookAuthor: String? = stringNullable(BookTable.AUTHOR)
      var currentFile = File(string(BookTable.CURRENT_MEDIA_PATH))
      val bookSpeed: Float = float(BookTable.PLAYBACK_SPEED)
      val bookRoot: String = string(BookTable.ROOT)
      val bookTime: Int = int(BookTable.TIME)
      val bookType: String = string(BookTable.TYPE)
      val loudnessGain = intNullable(BookTable.LOUDNESS_GAIN) ?: 0

      val chapters = db.query(table = ChapterTable.TABLE_NAME,
          columns = listOf(ChapterTable.NAME, ChapterTable.DURATION, ChapterTable.PATH, ChapterTable.LAST_MODIFIED, ChapterTable.MARKS),
          selection = "${ChapterTable.BOOK_ID} =?",
          selectionArgs = listOf(bookId))
          .mapRows {
            val name: String = string(ChapterTable.NAME)
            val duration: Int = int(ChapterTable.DURATION)
            val path: String = string(ChapterTable.PATH)
            val lastModified = long(ChapterTable.LAST_MODIFIED)
            val chapterMarks = stringNullable(ChapterTable.MARKS)?.let {
              chapterMarkAdapter.fromJson(it)!!
            } ?: emptySparseArray()
            Chapter(File(path), name, duration, lastModified, chapterMarks)
          }

      if (chapters.find { it.file == currentFile } == null) {
        e { "Couldn't get current file. Return first file" }
        currentFile = chapters[0].file
      }

      Book(bookId, Book.Type.valueOf(bookType), bookAuthor, currentFile, bookTime, bookName, chapters, bookSpeed, bookRoot, loudnessGain)
    }
  }

  fun activeBooks() = books(true)

  fun orphanedBooks() = books(false)

  private fun setBookVisible(bookId: Long, visible: Boolean): Int {
    val cv = ContentValues().apply {
      put(BookTable.ACTIVE, if (visible) 1 else 0)
    }
    return db.update(BookTable.TABLE_NAME, cv, "${BookTable.ID} =?", bookId)
  }

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
    put(ChapterTable.LAST_MODIFIED, fileLastModified)
    val markValue = chapterMarkAdapter.toJson(marks)
    put(ChapterTable.MARKS, markValue)
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
    put(BookTable.LOUDNESS_GAIN, loudnessGain)
  }

  fun updateBook(book: Book) = db.asTransaction {
    require(book.id != -1L) { "Book $book has an invalid id" }

    // update book itself
    val bookCv = book.toContentValues()
    update(BookTable.TABLE_NAME, bookCv, "${BookTable.ID}=?", book.id)

    // delete old chapters and replace them with new ones
    delete(ChapterTable.TABLE_NAME, "${BookTable.ID}=?", book.id)
    book.chapters.forEach { insert(it, book.id) }
  }

  fun addBook(toAdd: Book) = db.asTransaction {
    val bookCv = toAdd.toContentValues()
    val bookId = insertOrThrow(BookTable.TABLE_NAME, null, bookCv)
    val newBook = toAdd.copy(id = bookId)
    newBook.chapters.forEach { insert(it, bookId) }
    return@asTransaction newBook
  }
}
