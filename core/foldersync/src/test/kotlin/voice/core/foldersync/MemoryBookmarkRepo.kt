package voice.core.foldersync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.Bookmark
import voice.core.data.repo.BookmarkRepo
import java.time.Instant

class MemoryBookmarkRepo(vararg bookmarks: Bookmark) : BookmarkRepo {

  val bookmarks = MutableStateFlow(bookmarks.toList())

  override suspend fun deleteBookmark(id: Bookmark.Id) {
    bookmarks.update { bookmarks -> bookmarks.filterNot { it.id == id } }
  }

  override suspend fun addBookmark(bookmark: Bookmark) {
    bookmarks.update { bookmarks -> bookmarks.filterNot { it.id == bookmark.id } + bookmark }
  }

  override suspend fun addBookmarkAtBookPosition(
    book: Book,
    title: String?,
  ): Bookmark {
    val bookmark = Bookmark(
      bookId = book.id,
      chapterId = book.content.currentChapter,
      title = title,
      time = book.content.positionInChapter,
      addedAt = Instant.EPOCH,
      id = Bookmark.Id.random(),
    )
    addBookmark(bookmark)
    return bookmark
  }

  override suspend fun bookmarks(book: BookContent): List<Bookmark> = bookmarks.value.filter { it.chapterId in book.chapters }

  override fun bookmarksFlow(book: BookContent): Flow<List<Bookmark>> = bookmarks.map { bookmarks(book) }

  override fun flow(): Flow<List<Bookmark>> = bookmarks
}
