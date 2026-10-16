package voice.core.data.repo

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.Bookmark
import voice.core.data.repo.internals.AppDb
import voice.core.data.repo.internals.dao.BookmarkDao
import voice.core.data.repo.internals.transaction
import voice.core.data.runForMaxSqlVariableNumber
import java.time.Clock

@ContributesBinding(AppScope::class)
public class BookmarkRepoImpl
internal constructor(
  private val dao: BookmarkDao,
  private val appDb: AppDb,
  private val clock: Clock,
) : BookmarkRepo {

  override suspend fun deleteBookmark(id: Bookmark.Id) {
    dao.deleteBookmark(id)
  }

  override suspend fun addBookmark(bookmark: Bookmark) {
    dao.addBookmark(bookmark)
  }

  override suspend fun addBookmarkAtBookPosition(
    book: Book,
    title: String?,
  ): Bookmark {
    return withContext(Dispatchers.IO) {
      val bookMark = Bookmark(
        title = title,
        time = book.content.positionInChapter,
        id = Bookmark.Id.random(),
        addedAt = clock.instant(),
        chapterId = book.content.currentChapter,
        bookId = book.id,
      )
      addBookmark(bookMark)
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

  override fun flow(): Flow<List<Bookmark>> {
    return appDb.invalidationTracker.createFlow(BOOKMARK_TABLE)
      .map { dao.all() }
  }

  private companion object {
    const val BOOKMARK_TABLE = "bookmark2"
  }
}
