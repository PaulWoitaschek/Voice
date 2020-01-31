package de.ph1b.audiobook.playback.session

import android.net.Uri
import java.util.UUID
import javax.inject.Inject

/**
 * Helper class for converting book and chapter ids to media ids.
 */

private const val SCHEME = "voice"

class BookUriConverter
@Inject constructor() {

  private val baseUri = Uri.Builder().scheme(SCHEME).build()

  fun allBooksId(): String {
    return baseUri.toString()
  }

  fun chapterId(bookId: UUID, chapterId: Long): String {
    return baseUri.buildUpon()
      .appendPath(bookId.toString())
      .appendPath(chapterId.toString())
      .toString()
  }

  fun bookId(id: UUID): String {
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

    val bookId = pathSegments.firstOrNull()?.toUuidOrNull()
      ?: return Parsed.AllBooks

    val chapterId = pathSegments.getOrNull(1)?.toLongOrNull()
    return if (chapterId == null) {
      Parsed.Book(bookId)
    } else {
      Parsed.Chapter(bookId, chapterId)
    }
  }

  private fun String?.toUuidOrNull(): UUID? {
    return try {
      UUID.fromString(this)
    } catch (e: IllegalArgumentException) {
      null
    }
  }

  sealed class Parsed {
    object AllBooks : Parsed()
    data class Book(val id: UUID) : Parsed()
    data class Chapter(val bookId: UUID, val chapterId: Long) : Parsed()
  }
}
