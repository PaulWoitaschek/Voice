package voice.core.scanner

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.Bookmark
import voice.core.data.repo.BookmarkRepo
import java.time.Instant

class MemoryBookmarkRepo : BookmarkRepo {

  private val bookmarks = MutableStateFlow(mapOf<Bookmark.Id, Bookmark>())

  val all: List<Bookmark> get() = bookmarks.value.values.toList()

  override suspend fun deleteBookmark(id: Bookmark.Id) {
    bookmarks.update { it - id }
  }

  override suspend fun addBookmark(bookmark: Bookmark) {
    bookmarks.update { it + (bookmark.id to bookmark) }
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
    return bookmarks.value.values.filter { it.chapterId in book.chapters }
  }

  override fun bookmarksFlow(book: BookContent): Flow<List<Bookmark>> {
    return bookmarks.map { all -> all.values.filter { it.chapterId in book.chapters } }
  }
}
