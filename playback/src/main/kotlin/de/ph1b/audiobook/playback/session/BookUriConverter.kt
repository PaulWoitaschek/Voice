package de.ph1b.audiobook.playback.session

import android.net.Uri
import androidx.core.net.toUri
import javax.inject.Inject

private const val SCHEME = "voice"

class BookUriConverter
@Inject constructor() {

  private val baseUri = Uri.Builder().scheme(SCHEME).build()

  fun allBooksId(): String {
    return baseUri.toString()
  }

  fun chapterId(bookId: Uri, chapterId: Uri): String {
    return baseUri.buildUpon()
      .appendPath(bookId.toString())
      .appendPath(chapterId.toString())
      .toString()
  }

  fun bookId(id: Uri): String {
    return baseUri.buildUpon()
      .appendPath(id.toString())
      .toString()
  }

  fun parse(id: String): Parsed? {
    val uri = Uri.parse(id)
    if (uri.scheme != SCHEME) {
      return null
    }
    val pathSegments = uri.pathSegments

    val bookId = pathSegments.firstOrNull()?.toUri()
      ?: return Parsed.AllBooks

    val chapterId = pathSegments.getOrNull(1)?.toUri()
    return if (chapterId == null) {
      Parsed.Book(bookId)
    } else {
      Parsed.Chapter(bookId, chapterId)
    }
  }

  sealed class Parsed {
    object AllBooks : Parsed()
    data class Book(val id: Uri) : Parsed()
    data class Chapter(val bookId: Uri, val chapterId: Uri) : Parsed()
  }
}
