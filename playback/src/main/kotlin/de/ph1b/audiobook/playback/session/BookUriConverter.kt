package de.ph1b.audiobook.playback.session

import android.net.Uri
import androidx.core.net.toUri
import de.ph1b.audiobook.data.Book2
import javax.inject.Inject

private const val SCHEME = "voice"

class BookUriConverter
@Inject constructor() {

  private val baseUri = Uri.Builder().scheme(SCHEME).build()

  fun allBooksId(): String = baseUri.toString()

  fun bookId(id: Book2.Id): String {
    return baseUri.buildUpon()
      .appendPath(id.value)
      .toString()
  }

  fun parse(id: String): Parsed? {
    val uri = Uri.parse(id)
    if (uri.scheme != SCHEME) {
      return null
    }
    val pathSegments = uri.pathSegments

    val bookId = pathSegments.firstOrNull()?.toUri()?.let(Book2::Id)

    return if (bookId == null) {
      Parsed.AllBooks
    } else {
      Parsed.Book(bookId)
    }
  }

  sealed class Parsed {
    object AllBooks : Parsed()
    data class Book(val id: Book2.Id) : Parsed()
  }
}
