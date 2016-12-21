package de.ph1b.audiobook.playback.utils

import android.graphics.Bitmap
import de.ph1b.audiobook.Book

/**
 * A cache entry for a bitmap
 *
 * @author Paul Woitaschek
 */
data class CachedImage(val bookId: Long, val cover: Bitmap) {
  fun matches(book: Book) = book.id == bookId
}