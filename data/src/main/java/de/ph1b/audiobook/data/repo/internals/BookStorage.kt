package de.ph1b.audiobook.data.repo.internals

import androidx.room.RoomDatabase
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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

  suspend fun books(): List<Book> = synchronizedWithIoDispatcher {
    appDb.transaction {
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

  suspend fun revealBook(bookId: UUID) = synchronizedWithIoDispatcher {
    bookSettingsDao.setActive(bookId, true)
  }

  suspend fun hideBook(bookId: UUID) = synchronizedWithIoDispatcher {
    bookSettingsDao.setActive(bookId, false)
  }

  suspend fun addOrUpdate(book: Book) = synchronizedWithIoDispatcher {
    appDb.transaction {
      metaDataDao.insert(book.metaData)
      bookSettingsDao.insert(book.content.settings)
      // delete old chapters and replace them with new ones
      chapterDao.deleteByBookId(book.id)
      chapterDao.insert(book.content.chapters)
    }
  }

  private suspend inline fun <T> synchronizedWithIoDispatcher(crossinline action: () -> T): T {
    return mutex.withLock {
      withContext(IO) {
        action()
      }
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

private val mutex = Mutex()
