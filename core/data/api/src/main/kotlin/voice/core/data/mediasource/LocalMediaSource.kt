package voice.core.data.mediasource

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookSearchRepo
import voice.core.data.repo.BookmarkRepo
import voice.core.data.repo.ChapterRepo

@SingleIn(AppScope::class)
@Inject
@ContributesBinding(AppScope::class)
public class LocalMediaSource(
  private val bookmarkRepo: BookmarkRepo,
  private val bookRepo: BookRepository,
  private val bookContentRepo: BookContentRepo,
  private val chapterRepo: ChapterRepo,
  private val bookSearch: BookSearchRepo,
) : VoiceMediaSource {
  override suspend fun search(query: String): List<Book> {
    return bookSearch.search(query)
  }

  override fun flowBooks(): Flow<List<Book>> {
    return bookRepo.flow()
  }

  override suspend fun allBooks(): List<Book> {
    return bookRepo.all()
  }

  override fun flowBook(id: BookId): Flow<Book?> {
    return bookRepo.flow(id)
  }

  override suspend fun getBook(id: BookId): Book? {
    return bookRepo.get(id)
  }

  override suspend fun updateBook(id: BookId, update: (BookContent) -> BookContent) {
    return bookRepo.updateBook(id, update)
  }

  override fun flowBookContents(): Flow<List<BookContent>> {
    return bookContentRepo.flow()
  }

  override suspend fun allBookContents(): List<BookContent> {
    return bookContentRepo.all()
  }

  override fun flowBookContent(id: BookId): Flow<BookContent?> {
    return bookContentRepo.flow(id)
  }

  override suspend fun getBookContent(id: BookId): BookContent? {
    return bookContentRepo.get(id)
  }

  override suspend fun setAllBooksInactiveExcept(ids: List<BookId>) {
    return bookContentRepo.setAllInactiveExcept(ids)
  }

  override suspend fun putBookContent(content: BookContent) {
    return bookContentRepo.put(content)
  }

  override suspend fun deleteBookmark(id: Bookmark.Id) {
    bookmarkRepo.deleteBookmark(id)
  }

  override suspend fun addBookmark(bookmark: Bookmark) {
    return addBookmark(bookmark)
  }

  override suspend fun addBookmarkAtBookPosition(
    book: Book,
    title: String?,
    setBySleepTimer: Boolean,
  ): Bookmark {
    return addBookmarkAtBookPosition(book, title, setBySleepTimer)
  }

  override suspend fun bookmarks(book: BookContent): List<Bookmark> {
    return bookmarks(book)
  }

  public suspend fun getChapter(id: ChapterId): Chapter? {
    return chapterRepo.get(id)
  }

  public suspend fun putChapter(chapter: Chapter) {
    chapterRepo.put(chapter)
  }
}
