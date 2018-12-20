package de.ph1b.audiobook.data.repo.internals

import androidx.room.RoomDatabase
import de.ph1b.audiobook.crashreporting.CrashReporter
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.Executors
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

  suspend fun books(): List<Book> {
    return synchronizedWithIoDispatcher {
      appDb.transaction {
        bookSettingsDao.all()
          .map { bookSettings ->
            val bookId = bookSettings.id
            val metaData = metaDataDao.byId(bookId)
            val chapters = chapterDao.byBookId(bookId)

            val bookSettingsFileInChapters = chapters.any { chapter ->
              chapter.file == bookSettings.currentFile
            }
            val correctedBookSettings = if (bookSettingsFileInChapters) {
              bookSettings
            } else {
              Timber.e("bookSettings=$bookSettings currentFile is not in $chapters. metaData=$metaData")
              CrashReporter.logException(AssertionError())
              bookSettings.copy(currentFile = chapters[0].file)
            }

            Book(
              id = bookId, content = BookContent(
                id = bookId,
                chapters = chapters,
                settings = correctedBookSettings
              ),
              metaData = metaData
            )
          }
      }
    }
  }

  suspend fun revealBook(bookId: UUID) {
    synchronizedWithIoDispatcher {
      bookSettingsDao.setActive(bookId, true)
    }
  }

  suspend fun hideBook(bookId: UUID) {
    synchronizedWithIoDispatcher {
      bookSettingsDao.setActive(bookId, false)
    }
  }

  suspend fun addOrUpdate(book: Book) {
    synchronizedWithIoDispatcher {
      appDb.transaction {
        metaDataDao.insert(book.metaData)
        bookSettingsDao.insert(book.content.settings)
        // delete old chapters and replace them with new ones
        chapterDao.deleteByBookId(book.id)
        chapterDao.insert(book.content.chapters)
      }
    }
  }

  private suspend inline fun <T> synchronizedWithIoDispatcher(crossinline action: () -> T): T {
    // as the dispatcher is single threaded, we have implicit thread safety.
    return withContext(STORAGE_DISPATCHER) {
      action()
    }
  }

  private inline fun <T> RoomDatabase.transaction(action: () -> T): T {
    beginTransaction()
    return try {
      action().also {
        setTransactionSuccessful()
      }
    } finally {
      endTransaction()
    }
  }
}

private val STORAGE_DISPATCHER = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
