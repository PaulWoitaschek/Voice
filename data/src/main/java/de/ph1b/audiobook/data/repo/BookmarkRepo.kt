package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.repo.internals.AppDb
import de.ph1b.audiobook.data.repo.internals.BookmarkDao
import de.ph1b.audiobook.data.repo.internals.transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.Instant
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides access to bookmarks.
 */
@Singleton
class BookmarkRepo
@Inject constructor(
  private val dao: BookmarkDao,
  private val appDb: AppDb
) {

  suspend fun deleteBookmark(id: UUID) {
    dao.deleteBookmark(id)
  }

  suspend fun addBookmark(bookmark: Bookmark) {
    dao.addBookmark(bookmark)
  }

  suspend fun addBookmarkAtBookPosition(book: Book, title: String?, setBySleepTimer: Boolean): Bookmark {
    return withContext(Dispatchers.IO) {
      val bookMark = Bookmark(
        mediaFile = book.content.currentChapter.file,
        title = title,
        time = book.content.positionInChapter,
        id = UUID.randomUUID(),
        addedAt = Instant.now(),
        setBySleepTimer = setBySleepTimer
      )
      addBookmark(bookMark)
      Timber.v("Added bookmark=$bookMark")
      bookMark
    }
  }

  suspend fun bookmarks(book: Book): List<Bookmark> {
    val files = book.content.chapters.map {
      it.file
    }
    // we can only query SQLITE_MAX_VARIABLE_NUMBER at once (999 bugs on some devices so we use a number a little smaller.)
    // if it's larger than the limit, we query in chunks.
    val limit = 990
    return if (files.size > limit) {
      appDb.transaction {
        files.chunked(limit).flatMap {
          dao.allForFiles(it)
        }
      }
    } else {
      dao.allForFiles(files)
    }
  }
}
