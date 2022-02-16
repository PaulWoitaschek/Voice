package voice.data.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.data.Book
import voice.data.BookContent
import voice.data.Bookmark
import voice.data.repo.internals.AppDb
import voice.data.repo.internals.dao.BookmarkDao
import voice.data.repo.internals.transaction
import voice.logging.core.Logger
import java.time.Instant
import javax.inject.Inject

class BookmarkRepo
@Inject constructor(
  private val dao: BookmarkDao,
  private val appDb: AppDb
) {

  suspend fun deleteBookmark(id: Bookmark.Id) {
    dao.deleteBookmark(id)
  }

  suspend fun addBookmark(bookmark: Bookmark) {
    dao.addBookmark(bookmark)
  }

  suspend fun addBookmarkAtBookPosition(book: Book, title: String?, setBySleepTimer: Boolean): Bookmark {
    return withContext(Dispatchers.IO) {
      val bookMark = Bookmark(
        title = title,
        time = book.content.positionInChapter,
        id = Bookmark.Id.random(),
        addedAt = Instant.now(),
        setBySleepTimer = setBySleepTimer,
        chapterId = book.content.currentChapter,
        bookId = book.id
      )
      addBookmark(bookMark)
      Logger.v("Added bookmark=$bookMark")
      bookMark
    }
  }

  suspend fun bookmarks(book: BookContent): List<Bookmark> {
    val chapters = book.chapters
    // we can only query SQLITE_MAX_VARIABLE_NUMBER at once (999 bugs on some devices so we use a number a little smaller.)
    // if it's larger than the limit, we query in chunks.
    val limit = 990
    return if (chapters.size > limit) {
      appDb.transaction {
        chapters.chunked(limit).flatMap {
          dao.allForChapters(it)
        }
      }
    } else {
      dao.allForChapters(chapters)
    }
  }
}
