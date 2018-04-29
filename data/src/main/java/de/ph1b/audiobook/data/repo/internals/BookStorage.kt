package de.ph1b.audiobook.data.repo.internals

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.db.SupportSQLiteOpenHelper
import android.arch.persistence.db.SupportSQLiteQueryBuilder
import android.arch.persistence.room.OnConflictStrategy
import android.content.ContentValues
import androidx.database.getFloat
import androidx.database.getInt
import androidx.database.getIntOrNull
import androidx.database.getLong
import androidx.database.getString
import androidx.database.getStringOrNull
import com.squareup.moshi.Moshi
import de.ph1b.audiobook.common.sparseArray.emptySparseArray
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.repo.internals.tables.BookTable
import de.ph1b.audiobook.data.repo.internals.tables.ChapterTable
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Provides access to the persistent storage for bookmarks.
 */
class BookStorage
@Inject constructor(
  helper: SupportSQLiteOpenHelper,
  moshi: Moshi
) {

  private val chapterMarkAdapter = SparseArrayAdapter<String>(moshi.adapter(String::class.java))

  private val db by lazy { helper.writableDatabase }

  private fun books(active: Boolean): List<Book> {
    return db.transaction {
      val queryAllBooks = SupportSQLiteQueryBuilder.builder(BookTable.TABLE_NAME)
        .columns(
          arrayOf(
            BookTable.ID,
            BookTable.NAME,
            BookTable.AUTHOR,
            BookTable.CURRENT_MEDIA_PATH,
            BookTable.PLAYBACK_SPEED,
            BookTable.ROOT,
            BookTable.TIME,
            BookTable.TYPE,
            BookTable.LOUDNESS_GAIN
          )
        )
        .selection("${BookTable.ACTIVE} =?", arrayOf(if (active) 1 else 0))
        .create()
      db.query(queryAllBooks)
        .mapRows {
          val bookId: Long = getLong(BookTable.ID)
          val bookName: String = getString(BookTable.NAME)
          val bookAuthor: String? = getStringOrNull(BookTable.AUTHOR)
          var currentFile = File(getString(BookTable.CURRENT_MEDIA_PATH))
          val bookSpeed: Float = getFloat(BookTable.PLAYBACK_SPEED)
          val bookRoot: String = getString(BookTable.ROOT)
          val bookTime: Int = getInt(BookTable.TIME)
          val bookType: String = getString(BookTable.TYPE)
          val loudnessGain = getIntOrNull(BookTable.LOUDNESS_GAIN) ?: 0

          val queryChapters = SupportSQLiteQueryBuilder.builder(ChapterTable.TABLE_NAME)
            .columns(
              arrayOf(
                ChapterTable.NAME,
                ChapterTable.DURATION,
                ChapterTable.PATH,
                ChapterTable.LAST_MODIFIED,
                ChapterTable.MARKS
              )
            )
            .selection("${ChapterTable.BOOK_ID} =?", arrayOf(bookId))
            .create()
          val chapters = db.query(queryChapters)
            .mapRows {
              val name: String = getString(ChapterTable.NAME)
              val duration: Int = getInt(ChapterTable.DURATION)
              val path: String = getString(ChapterTable.PATH)
              val lastModified = getLong(ChapterTable.LAST_MODIFIED)
              val chapterMarks = getStringOrNull(ChapterTable.MARKS)?.let {
                chapterMarkAdapter.fromJson(it)!!
              } ?: emptySparseArray()
              Chapter(File(path), name, duration, lastModified, chapterMarks)
            }

          if (chapters.find { it.file == currentFile } == null) {
            Timber.e("Couldn't get current file. Return first file")
            currentFile = chapters[0].file
          }

          Book(
            id = bookId,
            type = Book.Type.valueOf(bookType),
            author = bookAuthor,
            content = BookContent(
              id = bookId,
              currentFile = currentFile,
              positionInChapter = bookTime,
              chapters = chapters,
              playbackSpeed = bookSpeed,
              loudnessGain = loudnessGain
            ),
            name = bookName,

            root = bookRoot
          )
        }
    }
  }

  fun activeBooks() = books(true)

  fun orphanedBooks() = books(false)

  private fun setBookVisible(bookId: Long, visible: Boolean): Int {
    val cv = ContentValues().apply {
      put(BookTable.ACTIVE, if (visible) 1 else 0)
    }
    return db.update(
      BookTable.TABLE_NAME,
      OnConflictStrategy.FAIL,
      cv,
      "${BookTable.ID} =?",
      arrayOf(bookId)
    )
  }

  fun revealBook(bookId: Long) {
    setBookVisible(bookId, true)
  }

  fun hideBook(bookId: Long) {
    setBookVisible(bookId, false)
  }

  private fun SupportSQLiteDatabase.insert(chapter: Chapter, bookId: Long): Long {
    return insert(ChapterTable.TABLE_NAME, OnConflictStrategy.FAIL, chapter.toContentValues(bookId))
  }

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
    put(BookTable.CURRENT_MEDIA_PATH, content.currentFile.absolutePath)
    put(BookTable.PLAYBACK_SPEED, content.playbackSpeed)
    put(BookTable.ROOT, root)
    put(BookTable.TIME, content.positionInChapter)
    put(BookTable.TYPE, type.name)
    put(BookTable.LOUDNESS_GAIN, content.loudnessGain)
  }

  fun updateBook(book: Book) {
    db.transaction {
      require(book.id != -1L) { "Book $book has an invalid id" }

      // update book itself
      val bookCv = book.toContentValues()
      update(
        BookTable.TABLE_NAME,
        OnConflictStrategy.FAIL,
        bookCv,
        "${BookTable.ID}=?",
        arrayOf(book.id)
      )

      // delete old chapters and replace them with new ones
      delete(ChapterTable.TABLE_NAME, "${BookTable.ID}=?", arrayOf(book.id))
      book.content.chapters.forEach { insert(it, book.id) }
    }
  }

  fun addBook(toAdd: Book): Book {
    return db.transaction {
      val bookCv = toAdd.toContentValues()
      val bookId = insert(BookTable.TABLE_NAME, OnConflictStrategy.FAIL, bookCv)
      val newBook = toAdd.copy(id = bookId, content = toAdd.content.copy(id = bookId))
      newBook.content.chapters.forEach { insert(it, bookId) }
      return@transaction newBook
    }
  }
}
