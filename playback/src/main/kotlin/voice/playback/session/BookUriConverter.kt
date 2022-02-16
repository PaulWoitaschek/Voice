package voice.playback.session

import android.net.Uri
import voice.data.Chapter
import javax.inject.Inject
import voice.data.Book.Id as BookId
import voice.data.Chapter.Id as ChapterId

private const val SCHEME = "voice"
private const val BOOK_ID = "book"
private const val CHAPTER_ID = "chapter"
private const val BOOK_PATH = "book"

class BookUriConverter
@Inject constructor() {

  fun allBooksId(): String = uri { }

  fun book(book: BookId): String = uri {
    appendQueryParameter(BOOK_ID, book.value)
  }

  fun chapter(book: BookId, chapter: Chapter.Id): String = uri {
    appendQueryParameter(BOOK_ID, book.value)
    appendQueryParameter(CHAPTER_ID, chapter.value)
  }

  private inline fun uri(configure: Uri.Builder.() -> Unit): String {
    return Uri.Builder()
      .scheme(SCHEME)
      .appendPath(BOOK_PATH)
      .also(configure)
      .build()
      .toString()
  }

  fun parse(id: String): Parsed? {
    val uri = Uri.parse(id)
    if (uri.scheme != SCHEME) {
      return null
    }

    val bookId = uri.getQueryParameter(BOOK_ID)?.let(::BookId)
    val chapterId = uri.getQueryParameter(CHAPTER_ID)?.let(::ChapterId)

    return when {
      bookId == null -> Parsed.AllBooks
      chapterId != null -> Parsed.Chapter(bookId, chapterId)
      else -> Parsed.Book(bookId)
    }
  }

  sealed interface Parsed {

    object AllBooks : Parsed

    @JvmInline
    value class Book(val bookId: BookId) : Parsed

    data class Chapter(val bookId: BookId, val chapterId: ChapterId) : Parsed
  }
}
