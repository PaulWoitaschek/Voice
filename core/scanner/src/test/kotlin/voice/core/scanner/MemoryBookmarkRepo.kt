package voice.core.scanner

import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.Bookmark
import voice.core.data.repo.BookmarkRepo
import java.time.Instant

class MemoryBookmarkRepo : BookmarkRepo {

  private val bookmarks = mutableMapOf<Bookmark.Id, Bookmark>()

  val all: List<Bookmark> get() = bookmarks.values.toList()

  override suspend fun deleteBookmark(id: Bookmark.Id) {
    bookmarks -= id
  }

  override suspend fun addBookmark(bookmark: Bookmark) {
    bookmarks[bookmark.id] = bookmark
  }

  override suspend fun addBookmarkAtBookPosition(
    book: Book,
    title: String?,
    setBySleepTimer: Boolean,
  ): Bookmark {
    return Bookmark(
      bookId = book.id,
      chapterId = book.content.currentChapter,
      title = title,
      time = book.content.positionInChapter,
      addedAt = Instant.now(),
      setBySleepTimer = setBySleepTimer,
      id = Bookmark.Id.random(),
    ).also { addBookmark(it) }
  }

  override suspend fun bookmarks(book: BookContent): List<Bookmark> {
    return bookmarks.values.filter { it.chapterId in book.chapters }
  }
}
