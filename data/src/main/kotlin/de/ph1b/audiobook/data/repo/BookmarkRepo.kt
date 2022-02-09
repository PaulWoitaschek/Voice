package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.BookContent2
import de.ph1b.audiobook.data.Bookmark2
import de.ph1b.audiobook.data.repo.internals.AppDb
import de.ph1b.audiobook.data.repo.internals.dao.BookmarkDao2
import de.ph1b.audiobook.data.repo.internals.transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

class BookmarkRepo
@Inject constructor(
  private val dao: BookmarkDao2,
  private val appDb: AppDb
) {

  suspend fun deleteBookmark(id: Bookmark2.Id) {
    dao.deleteBookmark(id)
  }

  suspend fun addBookmark(bookmark: Bookmark2) {
    dao.addBookmark(bookmark)
  }

  suspend fun addBookmarkAtBookPosition(book: Book2, title: String?, setBySleepTimer: Boolean): Bookmark2 {
    return withContext(Dispatchers.IO) {
      val bookMark = Bookmark2(
        title = title,
        time = book.content.positionInChapter,
        id = Bookmark2.Id.random(),
        addedAt = Instant.now(),
        setBySleepTimer = setBySleepTimer,
        chapterId = book.content.currentChapter,
        bookId = book.id
      )
      addBookmark(bookMark)
      Timber.v("Added bookmark=$bookMark")
      bookMark
    }
  }

  suspend fun bookmarks(book: BookContent2): List<Bookmark2> {
    val chapters = book.chapters
    // we can only query SQLITE_MAX_VARIABLE_NUMBER at once (999 bugs on some devices so we use a number a little smaller.)
    // if it's larger than the limit, we query in chunks.
    val limit = 990
    return if (chapters.size > limit) {
      appDb.transaction {
        chapters.chunked(limit).flatMap {
          dao.allForFiles(it)
        }
      }
    } else {
      dao.allForFiles(chapters)
    }
  }
}
