package de.ph1b.audiobook.data.repo.internals

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.repo.internals.dao.BookMetaDataDao
import de.ph1b.audiobook.data.repo.internals.dao.BookSettingsDao
import de.ph1b.audiobook.data.repo.internals.dao.ChapterDao
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides access to the persistent storage for bookmarks.
 */
@Singleton
class BookStorage
@Inject constructor(
  private val chapterDao: ChapterDao,
  private val metaDataDao: BookMetaDataDao,
  private val bookSettingsDao: BookSettingsDao,
  private val appDb: AppDb
) {

  suspend fun books(): List<Book> {
    return appDb.transaction {
      bookSettingsDao.all()
        .mapNotNull { bookSettings ->
          val bookId = bookSettings.id
          val metaData = metaDataDao.byId(bookId)
          val chapters = chapterDao.byBookId(bookId)
          if (chapters.isEmpty()) {
            Timber.e("No chapters found for metaData=$metaData, bookSettings=$bookSettings")
            metaDataDao.delete(metaData)
            bookSettingsDao.delete(bookSettings)
            null
          } else {
            val bookSettingsFileInChapters = chapters.any { chapter ->
              chapter.file == bookSettings.currentFile
            }
            val correctedBookSettings = if (bookSettingsFileInChapters) {
              bookSettings
            } else {
              Timber.e("bookSettings=$bookSettings currentFile is not in $chapters. metaData=$metaData")
              bookSettings.copy(currentFile = chapters[0].file)
            }

            Book(
              id = bookId,
              content = BookContent(
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

  suspend fun setBookActive(bookId: UUID, active: Boolean) {
    bookSettingsDao.setActive(bookId, active = active)
  }

  suspend fun addOrUpdate(book: Book) {
    appDb.transaction {
      metaDataDao.insert(book.metaData)
      bookSettingsDao.insert(book.content.settings)
      // delete old chapters and replace them with new ones
      chapterDao.deleteByBookId(book.id)
      chapterDao.insert(book.content.chapters)
    }
  }

  suspend fun updateBookName(id: UUID, name: String) {
    metaDataDao.updateBookName(id, name)
  }

  suspend fun updateLastPlayedAt(id: UUID, lastPlayedAt: Long) {
    bookSettingsDao.updateLastPlayedAt(id, lastPlayedAt)
  }

  suspend fun updateBookContent(content: BookContent) {
    bookSettingsDao.insert(content.settings)
  }
}
