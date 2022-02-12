package de.ph1b.audiobook.playback.session

import android.net.Uri
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.Chapter2
import javax.inject.Inject

private const val SCHEME = "voice"
private const val BOOK_ID = "book"
private const val CHAPTER_ID = "chapter"
private const val BOOK_PATH = "book"

class BookUriConverter
@Inject constructor() {

  fun allBooksId(): String = uri { }

  fun book(book: Book2.Id): String = uri {
    appendQueryParameter(BOOK_ID, book.value)
  }

  fun chapter(book: Book2.Id, chapter: Chapter2.Id): String = uri {
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

    val bookId = uri.getQueryParameter(BOOK_ID)?.let(Book2::Id)
    val chapterId = uri.getQueryParameter(CHAPTER_ID)?.let(Chapter2::Id)

    return when {
      bookId == null -> Parsed.AllBooks
      chapterId != null -> Parsed.Chapter(bookId, chapterId)
      else -> Parsed.Book(bookId)
    }
  }

  sealed interface Parsed {

    object AllBooks : Parsed

    @JvmInline
    value class Book(val bookId: Book2.Id) : Parsed

    data class Chapter(val bookId: Book2.Id, val chapterId: Chapter2.Id) : Parsed
  }
}
