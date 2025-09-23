package voice.core.data.mediasource

import kotlinx.coroutines.flow.Flow
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.repo.BookSearchRepo
import voice.core.data.repo.BookmarkRepo

public interface VoiceMediaSource : BookmarkRepo, BookSearchRepo/*, BookRepository, BookContentRepo, ChapterRepo*/ {

  public fun flowBooks(): Flow<List<Book>>

  public suspend fun allBooks(): List<Book>

  public fun flowBook(id: BookId): Flow<Book?>

  public suspend fun getBook(id: BookId): Book?

  public suspend fun updateBook(
    id: BookId,
    update: (BookContent) -> BookContent,
  )

  public fun flowBookContents(): Flow<List<BookContent>>

  public suspend fun allBookContents(): List<BookContent>
  public fun flowBookContent(id: BookId): Flow<BookContent?>

  public suspend fun getBookContent(id: BookId): BookContent?

  public suspend fun setAllBooksInactiveExcept(ids: List<BookId>)

  public suspend fun putBookContent(content: BookContent)

}
