package voice.playback.notification

import android.graphics.Bitmap
import voice.data.Book
import java.io.File

data class CachedImage(val file: File?, val cover: Bitmap) {

  fun matches(book: Book) = book.content.cover == file
}
