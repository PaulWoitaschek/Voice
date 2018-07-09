package de.ph1b.audiobook.data.repo.internals

import android.arch.persistence.room.RoomDatabase
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import java.util.UUID
import javax.inject.Inject

/**
 * Provides access to the persistent storage for bookmarks.
 */
class BookStorage
@Inject constructor(
  private val chapterDao: ChapterDao,
  private val metaDataDao: BookMetaDataDao,
  private val bookSettingsDao: BookSettingsDao,
  private val appDb: AppDb
) {

  fun books(): List<Book> {
    return appDb.transaction {
      bookSettingsDao.all()
        .map { bookSettings ->
          val bookId = bookSettings.id
          val metaData = metaDataDao.byId(bookId)
          val chapters = chapterDao.byBookId(bookId)

          Book(
            id = bookId, content = BookContent(
              id = bookId,
              chapters = chapters,
              settings = bookSettings
            ),
            metaData = metaData
          )
        }
    }
  }

  fun revealBook(bookId: UUID) {
    bookSettingsDao.setActive(bookId, true)
  }

  fun hideBook(bookId: UUID) {
    bookSettingsDao.setActive(bookId, false)
  }

  fun addOrUpdate(book: Book) {
    appDb.transaction {
      metaDataDao.insert(book.metaData)
      bookSettingsDao.insert(book.content.settings)
      // delete old chapters and replace them with new ones
      chapterDao.deleteByBookId(book.id)
      chapterDao.insert(book.content.chapters)
    }
  }

  private inline fun <T> RoomDatabase.transaction(action: () -> T): T {
    beginTransaction()
    return try {
      val result = action()
      setTransactionSuccessful()
      result
    } finally {
      endTransaction()
    }
  }
}
