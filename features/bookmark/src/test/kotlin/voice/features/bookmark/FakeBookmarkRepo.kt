package voice.features.bookmark

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.Bookmark
import voice.core.data.repo.BookmarkRepo
import java.time.Clock

internal class FakeBookmarkRepo(private val clock: Clock) : BookmarkRepo {

  val bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())

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
      addedAt = clock.instant(),
      id = Bookmark.Id.random(),
    )
    addBookmark(bookmark)
    return bookmark
  }

  override suspend fun bookmarks(book: BookContent): List<Bookmark> = bookmarks.value

  override fun bookmarksFlow(book: BookContent): Flow<List<Bookmark>> = bookmarks

  override fun flow(): Flow<List<Bookmark>> = bookmarks
}
