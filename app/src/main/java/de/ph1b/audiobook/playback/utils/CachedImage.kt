package de.ph1b.audiobook.playback.utils

import android.graphics.Bitmap
import de.ph1b.audiobook.data.Book
import java.util.UUID

/**
 * A cache entry for a bitmap
 */
data class CachedImage(val bookId: UUID, val cover: Bitmap) {

  fun matches(book: Book) = book.id == bookId
}
