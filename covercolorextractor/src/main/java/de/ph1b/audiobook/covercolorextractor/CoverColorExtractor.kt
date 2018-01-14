package de.ph1b.audiobook.covercolorextractor

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v7.graphics.Palette
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import kotlinx.coroutines.experimental.withContext
import timber.log.Timber
import java.io.File
import kotlin.coroutines.experimental.suspendCoroutine

class CoverColorExtractor(private val picasso: Picasso) {

  private val tasks = HashMap<Long, Deferred<Int?>>()
  private val extractedColors = HashMap<Long, Int?>()

  suspend fun extract(file: File): Int? = withContext(IO) {
    if (!file.canRead()) {
      return@withContext null
    }
    val fileHash = fileHash(file)
    val extracted = extractedColors.getOrElse(fileHash) { 0 }
    if (extracted == 0) {
      val task = tasks.getOrPut(fileHash) { extractionTask(file) }
      task.await()
    } else extracted
  }

  private suspend fun extractionTask(file: File): Deferred<Int?> = async {
    val bitmap = bitmapByFile(file)
    if (bitmap != null) {
      val extracted = extractColor(bitmap)
      bitmap.recycle()
      val hash = fileHash(file)
      extractedColors.put(hash, extracted)
      extracted
    } else null
  }

  private suspend fun bitmapByFile(file: File): Bitmap? = withContext(UI) {
    suspendCoroutine<Bitmap?> { cont ->
      Timber.i("load cover for $file")
      picasso
          .load(file)
          .memoryPolicy(MemoryPolicy.NO_STORE)
          .resize(500, 500)
          .into(object : Target {
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            }

            override fun onBitmapFailed(errorDrawable: Drawable?) {
              cont.resume(null)
            }

            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
              cont.resume(bitmap)
            }
          })
    }
  }

  private suspend fun extractColor(bitmap: Bitmap): Int? =
      suspendCancellableCoroutine { cont ->
        Palette.from(bitmap)
            .generate {
              val invalidColor = -1
              val color = it.getVibrantColor(invalidColor)
              cont.resume(color.takeUnless { it == invalidColor })
            }
      }

  private fun fileHash(file: File) = file.absolutePath.hashCode() + file.lastModified() + file.length()
}
