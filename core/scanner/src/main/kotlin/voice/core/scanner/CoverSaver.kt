package voice.core.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.core.graphics.scale
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.logging.api.Logger
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.uuid.Uuid

// kept out of the cloud backup, as the scan finds these covers again. Covers listeners picked in 26.10.2 or earlier
// are in here too, as they had no folder of their own.
private const val EXTRACTED_COVERS = "bookCovers"

private const val CUSTOM_COVERS = "customCovers"

private const val MAX_COVER_SIZE = 1200

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
    val newCover = withContext(Dispatchers.IO) {
      cover.saveAsWebp(CUSTOM_COVERS)
    } ?: return

    repo.get(bookId)?.content?.customCover?.deleteOnIo()
    repo.updateBook(bookId) {
      it.copy(customCover = newCover)
    }
  }

  internal suspend fun forgetCustomCover(
    bookId: BookId,
    cover: File,
  ) {
    repo.updateBook(bookId) {
      if (it.customCover == cover) it.copy(customCover = null) else it
    }
  }

  internal fun newTemporaryFile(): File = File(context.cacheDir, "cover-${Uuid.random()}")

  internal suspend fun saveExtractedCover(
    image: File,
    bookId: BookId,
  ): Boolean {
    val cover = withContext(Dispatchers.IO) {
      decode(image)?.saveAsWebp(EXTRACTED_COVERS)
    } ?: return false

    repo.get(bookId)?.content?.metadata?.cover?.deleteOnIo()
    repo.updateBook(bookId) {
      it.copy(metadata = it.metadata.copy(cover = cover))
    }
    return true
  }

  // ImageDecoder turns a photo upright as its EXIF data says, which BitmapFactory doesn't
  private fun decode(image: File): Bitmap? {
    return try {
      ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(image.readBytes()))) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        // decoding a large embedded image at full size could run out of memory
        var sampleSize = 1
        while (max(info.size.width, info.size.height) / (sampleSize * 2) >= MAX_COVER_SIZE) {
          sampleSize *= 2
        }
        decoder.setTargetSampleSize(sampleSize)
      }
    } catch (_: IOException) {
      null
    }
  }

  private fun Bitmap.saveAsWebp(folder: String): File? {
    val longestSide = max(width, height)
    val scaled = if (longestSide > MAX_COVER_SIZE) {
      scale(
        width = (width * MAX_COVER_SIZE / longestSide).coerceAtLeast(1),
        height = (height * MAX_COVER_SIZE / longestSide).coerceAtLeast(1),
      )
    } else {
      this
    }
    val format = if (Build.VERSION.SDK_INT >= 30) {
      Bitmap.CompressFormat.WEBP_LOSSY
    } else {
      @Suppress("DEPRECATION")
      Bitmap.CompressFormat.WEBP
    }
    val file = File(coversFolder(folder), "${Uuid.random()}.webp")
    val saved = try {
      file.outputStream().use { scaled.compress(format, 75, it) }
    } catch (e: IOException) {
      Logger.w(e, "Error at saving a cover image")
      false
    }
    if (!saved) file.delete()
    return file.takeIf { saved }
  }

  private fun coversFolder(name: String): File = File(context.filesDir, name).also { it.mkdirs() }

  private suspend fun File.deleteOnIo() {
    withContext(Dispatchers.IO) {
      delete()
    }
  }
}
