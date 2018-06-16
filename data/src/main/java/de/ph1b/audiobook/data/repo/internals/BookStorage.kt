package de.ph1b.audiobook.data.repo.internals

import android.arch.persistence.db.SupportSQLiteOpenHelper
import android.arch.persistence.db.SupportSQLiteQueryBuilder
import android.arch.persistence.room.OnConflictStrategy
import android.content.ContentValues
import androidx.core.database.getFloat
import androidx.core.database.getInt
import androidx.core.database.getIntOrNull
import androidx.core.database.getString
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.BookSettings
import de.ph1b.audiobook.data.repo.internals.tables.BookTable
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Provides access to the persistent storage for bookmarks.
 */
class BookStorage
@Inject constructor(
  helper: SupportSQLiteOpenHelper,
  private val chapterDao: ChapterDao,
  private val metaDataDao: BookMetaDataDao
) {

  private val db by lazy { helper.writableDatabase }

  private fun books(active: Boolean): List<Book> {
    return db.transaction {
      val queryAllBooks = SupportSQLiteQueryBuilder.builder(BookTable.TABLE_NAME)
        .columns(
          arrayOf(
            BookTable.ID,
            BookTable.CURRENT_MEDIA_PATH,
            BookTable.PLAYBACK_SPEED,
            BookTable.TIME,
            BookTable.LOUDNESS_GAIN,
            BookTable.SKIP_SILENCE
          )
        )
        .selection("${BookTable.ACTIVE} =?", arrayOf(if (active) 1 else 0))
        .create()
      db.query(queryAllBooks)
        .mapRows {
          val bookId: UUID = UUID.fromString(getString(BookTable.ID))
          var currentFile = File(getString(BookTable.CURRENT_MEDIA_PATH))
          val bookSpeed: Float = getFloat(BookTable.PLAYBACK_SPEED)
          val bookTime: Int = getInt(BookTable.TIME)
          val loudnessGain = getIntOrNull(BookTable.LOUDNESS_GAIN) ?: 0
          val skipSilence = getIntOrNull(BookTable.SKIP_SILENCE) == 1

          val chapters = chapterDao.byBookId(bookId)

          if (chapters.find { it.file == currentFile } == null) {
            Timber.e("Couldn't get current file. Return first file")
            currentFile = chapters[0].file
          }

          val metaData = metaDataDao.byId(bookId)

          Book(
            id = bookId,
            metaData = metaData,
            content = BookContent(
              id = bookId,
              settings = BookSettings(
                id = bookId,
                currentFile = currentFile,
                positionInChapter = bookTime,
                playbackSpeed = bookSpeed,
                loudnessGain = loudnessGain,
                skipSilence = skipSilence
              ),
              chapters = chapters
            )
          )
        }
    }
  }

  fun activeBooks() = books(true)

  fun orphanedBooks() = books(false)

  private fun setBookVisible(bookId: UUID, visible: Boolean): Int {
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

  fun revealBook(bookId: UUID) {
    setBookVisible(bookId, true)
  }

  fun hideBook(bookId: UUID) {
    setBookVisible(bookId, false)
  }

  private fun Book.toContentValues() = ContentValues().apply {
    put(BookTable.ACTIVE, 1)
    put(BookTable.CURRENT_MEDIA_PATH, content.currentFile.absolutePath)
    put(BookTable.PLAYBACK_SPEED, content.settings.playbackSpeed)
    put(BookTable.TIME, content.positionInChapter)
    put(BookTable.LOUDNESS_GAIN, content.settings.loudnessGain)
    put(BookTable.SKIP_SILENCE, content.settings.skipSilence)
    put(BookTable.ID, id.toString())
  }

  fun updateBook(book: Book) {
    db.transaction {
      // update book itself
      val bookCv = book.toContentValues()
      update(
        BookTable.TABLE_NAME,
        OnConflictStrategy.FAIL,
        bookCv,
        "${BookTable.ID}=?",
        arrayOf(book.id.toString())
      )

      metaDataDao.insert(book.metaData)

      // delete old chapters and replace them with new ones
      chapterDao.deleteByBookId(book.id)
      chapterDao.insert(book.content.chapters)
    }
  }

  fun addBook(toAdd: Book): Book {
    return db.transaction {
      val bookCv = toAdd.toContentValues()
      insert(BookTable.TABLE_NAME, OnConflictStrategy.FAIL, bookCv)
      metaDataDao.insert(toAdd.metaData)
      chapterDao.insert(toAdd.content.chapters)
      return@transaction toAdd
    }
  }
}
