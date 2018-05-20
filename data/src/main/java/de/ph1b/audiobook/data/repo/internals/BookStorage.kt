package de.ph1b.audiobook.data.repo.internals

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
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.BookMetaData
import de.ph1b.audiobook.data.repo.internals.tables.BookTable
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Provides access to the persistent storage for bookmarks.
 */
class BookStorage
@Inject constructor(
  helper: SupportSQLiteOpenHelper,
  private val chapterDao: ChapterDao
) {

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
            BookTable.LOUDNESS_GAIN,
            BookTable.SKIP_SILENCE
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
          val skipSilence = getIntOrNull(BookTable.SKIP_SILENCE) == 1

          val chapters = chapterDao.byBookId(bookId)

          if (chapters.find { it.file == currentFile } == null) {
            Timber.e("Couldn't get current file. Return first file")
            currentFile = chapters[0].file
          }

          Book(
            id = bookId,
            metaData = BookMetaData(
              id = bookId,
              type = Book.Type.valueOf(bookType),
              author = bookAuthor,
              name = bookName,
              root = bookRoot
            ),
            content = BookContent(
              id = bookId,
              currentFile = currentFile,
              positionInChapter = bookTime,
              chapters = chapters,
              playbackSpeed = bookSpeed,
              loudnessGain = loudnessGain,
              skipSilence = skipSilence
            )
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
    put(BookTable.SKIP_SILENCE, content.skipSilence)
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
      chapterDao.deleteByBookId(book.id)
      chapterDao.insert(book.content.chapters)
    }
  }

  fun addBook(toAdd: Book): Book {
    return db.transaction {
      val bookCv = toAdd.toContentValues()
      val bookId = insert(BookTable.TABLE_NAME, OnConflictStrategy.FAIL, bookCv)
      val oldContent = toAdd.content
      val oldMetaData = toAdd.metaData
      val newBook = toAdd.copy(
        id = bookId,
        content = oldContent.copy(
          id = bookId,
          chapters = oldContent.chapters.map {
            it.copy(bookId = bookId)
          }
        ),
        metaData = oldMetaData.copy(
            id = bookId
        )
      )
      chapterDao.insert(newBook.content.chapters)
      return@transaction newBook
    }
  }
}
