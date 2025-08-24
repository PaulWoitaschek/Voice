package voice.data.repo

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.data.Book
import voice.data.BookContent
import voice.data.Bookmark
import voice.data.repo.internals.AppDb
import voice.data.repo.internals.dao.BookmarkDao
import voice.data.repo.internals.transaction
import voice.data.runForMaxSqlVariableNumber
import voice.logging.core.Logger
import java.time.Instant

@Inject
public class BookmarkRepo
internal constructor(
  private val dao: BookmarkDao,
  private val appDb: AppDb,
) {

  public suspend fun deleteBookmark(id: Bookmark.Id) {
    dao.deleteBookmark(id)
  }

  public suspend fun addBookmark(bookmark: Bookmark) {
    dao.addBookmark(bookmark)
  }

  public suspend fun addBookmarkAtBookPosition(
    book: Book,
    title: String?,
    setBySleepTimer: Boolean,
  ): Bookmark {
    return withContext(Dispatchers.IO) {
      val bookMark = Bookmark(
        title = title,
        time = book.content.positionInChapter,
        id = Bookmark.Id.random(),
        addedAt = Instant.now(),
        setBySleepTimer = setBySleepTimer,
        chapterId = book.content.currentChapter,
        bookId = book.id,
      )
      addBookmark(bookMark)
      Logger.v("Added bookmark=$bookMark")
      bookMark
    }
  }

  public suspend fun bookmarks(book: BookContent): List<Bookmark> {
    val chapters = book.chapters
    return appDb.transaction {
      chapters.runForMaxSqlVariableNumber {
        dao.allForChapters(it)
      }
    }
  }
}
