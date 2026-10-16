package voice.core.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.graphics.scale
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.core.data.BookId
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.BookRepository
import voice.core.logging.api.Logger
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.uuid.Uuid

private const val CUSTOM_COVERS = "bookCovers"

// kept out of the cloud backup, as the scan finds these covers again
private const val EXTRACTED_COVERS = "extractedCovers"

private const val MAX_COVER_SIZE = 1200

@Inject
public class CoverSaver
internal constructor(
  private val repo: BookRepository,
  private val contentRepo: BookContentRepo,
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

  internal suspend fun newTemporaryFile(): File = withContext(Dispatchers.IO) {
    File.createTempFile("cover", null, context.cacheDir)
  }

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

  // extracted covers used to be stored next to the custom ones, which are backed up
  internal suspend fun moveExtractedCovers() {
    val contents = contentRepo.all()
    contents.mapNotNull { it.metadata.cover }
      .filter { it.parentFile?.name == CUSTOM_COVERS }
      .distinct()
      .forEach { old ->
        val moved = withContext(Dispatchers.IO) {
          File(coversFolder(EXTRACTED_COVERS), old.name)
            .also { old.renameTo(it) }
            .takeIf { it.exists() }
        } ?: return@forEach
        // a book that moved shares the file with its old entry
        contents.filter { it.metadata.cover == old }.forEach { content ->
          repo.updateBook(content.id) {
            if (it.metadata.cover == old) it.copy(metadata = it.metadata.copy(cover = moved)) else it
          }
        }
      }
  }

  private fun decode(image: File): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(image.path, bounds)
    // decoding a large embedded image at full size could run out of memory
    var sampleSize = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_COVER_SIZE) {
      sampleSize *= 2
    }
    return BitmapFactory.decodeFile(image.path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
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
