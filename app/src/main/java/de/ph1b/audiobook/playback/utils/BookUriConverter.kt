package de.ph1b.audiobook.playback.utils

import android.content.UriMatcher
import android.net.Uri
import java.util.UUID
import javax.inject.Inject

/**
 * Helper class for converting book and chapter ids to uris and back.
 */
class BookUriConverter
@Inject constructor() {

  private fun baseBuilder() = Uri.Builder()
    .authority(AUTHORITY)
    .appendPath(PATH_BOOKS)

  private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
    addURI(AUTHORITY, PATH_BOOKS, ROOT)
    addURI(AUTHORITY, "$PATH_BOOKS/#", BOOK_ID)
    addURI(AUTHORITY, "$PATH_BOOKS/#/$PATH_CHAPTERS/#", CHAPTER_ID)
  }

  fun type(uri: Uri): Int = matcher.match(uri)

  fun allBooks(): Uri = baseBuilder().build()

  fun book(bookId: UUID): Uri = baseBuilder()
    .appendPath(bookId.toString())
    .build()

  fun chapter(bookId: UUID, chapter: Int): Uri = baseBuilder()
    .appendPath(bookId.toString())
    .appendPath(PATH_CHAPTERS)
    .appendPath(chapter.toString())
    .build()

  fun extractBook(uri: Uri) = UUID.fromString(uri.pathSegments[1].toString())!!

  companion object {
    private const val AUTHORITY = "books"

    private const val PATH_BOOKS = "root"
    private const val PATH_CHAPTERS = "chapters"

    const val ROOT = 1
    const val BOOK_ID = 2
    const val CHAPTER_ID = 3
  }
}
