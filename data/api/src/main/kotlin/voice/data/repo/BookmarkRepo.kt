package voice.data.repo

import voice.data.Book
import voice.data.BookContent
import voice.data.Bookmark

public interface BookmarkRepo {
  public suspend fun deleteBookmark(id: Bookmark.Id)

  public suspend fun addBookmark(bookmark: Bookmark)

  public suspend fun addBookmarkAtBookPosition(
    book: Book,
    title: String?,
    setBySleepTimer: Boolean,
  ): Bookmark

  public suspend fun bookmarks(book: BookContent): List<Bookmark>
}
