package de.ph1b.audiobook.playback.utils

import android.content.UriMatcher
import android.net.Uri
import javax.inject.Inject

/**
 * Helper class for converting book and chapter ids to uris and back.
 */
class BookUriConverter
@Inject constructor() {

  private fun baseBuilder() = Uri.Builder()
      .authority(booksAuthority)
      .appendPath(PATH_BOOKS)

  private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
    addURI(booksAuthority, PATH_BOOKS, ROOT)
    addURI(booksAuthority, "$PATH_BOOKS/#", BOOK_ID)
  }

  fun match(uri: Uri) = matcher.match(uri)

  fun allBooks(): Uri = baseBuilder().build()

  fun book(bookId: Long): Uri = baseBuilder()
      .appendPath(bookId.toString())
      .build()

  fun extractBook(uri: Uri) = uri.pathSegments[1].toLong()

  companion object {
    private const val booksAuthority = "BOOKS"
    private const val PATH_BOOKS = "root"
    const val ROOT = 1
    const val BOOK_ID = 2
  }
}
