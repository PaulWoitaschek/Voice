package voice.scanner

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.scale
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.data.BookId
import voice.data.repo.BookRepository
import voice.logging.core.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlin.math.max

@Inject
public class CoverSaver
internal constructor(
  private val repo: BookRepository,
  private val context: Context,
) {

  public suspend fun save(
    bookId: BookId,
    cover: Bitmap,
  ) {
    val newCover = newBookCoverFile()

    withContext(Dispatchers.IO) {
      // scale down if bitmap is too large
      val preferredSize = 1920
      val bitmapToSave = if (max(cover.width, cover.height) > preferredSize) {
        cover.scale(preferredSize, preferredSize)
      } else {
        cover
      }

      try {
        FileOutputStream(newCover).use {
          val compressFormat = when (newCover.extension) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> if (Build.VERSION.SDK_INT >= 30) {
              Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
              @Suppress("DEPRECATION")
              Bitmap.CompressFormat.WEBP
            }
            else -> error("Unhandled image extension for $newCover")
          }
          bitmapToSave.compress(compressFormat, 70, it)
          it.flush()
        }
      } catch (e: IOException) {
        Logger.w(e, "Error at saving image with destination=$newCover")
      }
    }

    setBookCover(newCover, bookId)
  }

  internal suspend fun newBookCoverFile(): File {
    val coversFolder = withContext(Dispatchers.IO) {
      File(context.filesDir, "bookCovers")
        .also { coverFolder -> coverFolder.mkdirs() }
    }
    return File(coversFolder, "${UUID.randomUUID()}.png")
  }

  internal suspend fun setBookCover(
    cover: File,
    bookId: BookId,
  ) {
    val oldCover = repo.get(bookId)?.content?.cover
    if (oldCover != null) {
      withContext(Dispatchers.IO) {
        oldCover.delete()
      }
    }

    repo.updateBook(bookId) {
      it.copy(cover = cover)
    }
  }
}
