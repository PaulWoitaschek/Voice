package voice.core.data.repo

import androidx.room.RoomDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.Bookmark
import voice.core.data.ListeningEvent
import voice.core.data.repo.internals.dao.BookmarkDao
import voice.core.data.repo.internals.transaction
import voice.core.data.runForMaxSqlVariableNumber
import voice.core.logging.api.Logger
import java.time.Clock

@ContributesBinding(AppScope::class)
public class BookmarkRepoImpl
internal constructor(
  private val dao: BookmarkDao,
  private val appDb: RoomDatabase,
  private val listeningHistoryRepo: ListeningHistoryRepo,
  private val clock: Clock,
) : BookmarkRepo {

  override suspend fun deleteBookmark(id: Bookmark.Id) {
    val bookmark = dao.bookmark(id)
    dao.deleteBookmark(id)
    if (bookmark != null) {
      listeningHistoryRepo.add(
        ListeningEvent(
          bookId = bookmark.bookId,
          type = ListeningEvent.Type.BookmarkDeleted,
          source = ListeningEvent.Source.App,
          atMillis = clock.millis(),
          chapterId = bookmark.chapterId,
          time = bookmark.time,
          value = bookmark.title,
          bookmarkId = bookmark.id,
          bookmarkKind = bookmark.kind,
          bookmarkSetBySleepTimer = bookmark.setBySleepTimer,
        ),
      )
    }
  }

  override suspend fun addBookmark(bookmark: Bookmark) {
    dao.addBookmark(bookmark)
  }

  override suspend fun addBookmarkAtBookPosition(
    book: Book,
    title: String?,
    setBySleepTimer: Boolean,
  ): Bookmark {
    return withContext(Dispatchers.IO) {
      val bookMark = Bookmark(
        title = title,
        time = book.content.positionInChapter,
        id = Bookmark.Id.random(),
        addedAt = clock.instant(),
        setBySleepTimer = setBySleepTimer,
        chapterId = book.content.currentChapter,
        bookId = book.id,
      )
      addBookmark(bookMark)
      Logger.v("Added bookmark=$bookMark")
      if (!setBySleepTimer) {
        listeningHistoryRepo.add(
          ListeningEvent(
            bookId = book.id,
            type = ListeningEvent.Type.BookmarkAdded,
            source = ListeningEvent.Source.App,
            atMillis = bookMark.addedAt.toEpochMilli(),
            chapterId = bookMark.chapterId,
            time = bookMark.time,
            bookmarkId = bookMark.id,
            bookmarkKind = bookMark.kind,
          ),
        )
      }
      bookMark
    }
  }

  override suspend fun bookmarks(book: BookContent): List<Bookmark> {
    val chapters = book.chapters
    return appDb.transaction {
      chapters.runForMaxSqlVariableNumber {
        dao.allForChapters(it)
      }
    }
  }

  override fun bookmarksFlow(book: BookContent): Flow<List<Bookmark>> {
    return appDb.invalidationTracker.createFlow(BOOKMARK_TABLE)
      .map { bookmarks(book) }
  }

  private companion object {
    const val BOOKMARK_TABLE = "bookmark2"
  }
}
