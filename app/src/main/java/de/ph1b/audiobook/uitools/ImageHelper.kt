package de.ph1b.audiobook.uitools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

// 500 kb
const val MAX_IMAGE_SIZE = 500 * 1024

@Singleton
class ImageHelper
@Inject
constructor(private val windowManager: Provider<WindowManager>) {

  fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
  }

  suspend fun saveCover(bitmap: Bitmap, destination: File) = withContext(Dispatchers.IO) {
    var bitmapToSave = bitmap
    // make bitmap square
    val width = bitmapToSave.width
    val height = bitmapToSave.height
    val size = Math.min(width, height)
    if (width != height) {
      bitmapToSave = Bitmap.createBitmap(bitmapToSave, 0, 0, size, size)
    }

    // scale down if bitmap is too large
    val preferredSize = smallerScreenSize
    if (size > preferredSize) {
      bitmapToSave = Bitmap.createScaledBitmap(bitmapToSave, preferredSize, preferredSize, true)
    }

    // save bitmap to storage
    try {
      FileOutputStream(destination).use {
        bitmapToSave.compress(Bitmap.CompressFormat.WEBP, 70, it)
        it.flush()
      }
    } catch (e: IOException) {
      Timber.e(e, "Error at saving image with destination=$destination")
    }
  }

  val smallerScreenSize: Int
    @Suppress("deprecation")
    get() {
      val display = windowManager.get().defaultDisplay
      val displayWidth = display.width
      val displayHeight = display.height
      return if (displayWidth < displayHeight) displayWidth else displayHeight
    }

  fun getEmbeddedCover(f: File): Bitmap? {
    val mmr = MediaMetadataRetriever()
    try {
      mmr.setDataSource(f.absolutePath)
      val data = mmr.embeddedPicture
      if (data != null) {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(data, 0, data.size, options)
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options)
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(data, 0, data.size, options)
      }
    } catch (ignored: RuntimeException) {
    }

    return null
  }

  private fun calculateInSampleSize(options: BitmapFactory.Options): Int {

    // Raw height and width of image
    val height = options.outHeight
    val width = options.outWidth
    var reqLength = smallerScreenSize

    // setting reqWidth matching to desired 1:1 ratio and screen-size
    reqLength *= if (width < height) {
      (height / width)
    } else {
      (width / height)
    }

    var inSampleSize = 1

    if (height > reqLength || width > reqLength) {
      val halfHeight = height / 2
      val halfWidth = width / 2

      // Calculate the largest inSampleSize value that is a power of 2 and keeps both
      // height and width larger than the requested height and width.
      while ((halfHeight / inSampleSize) > reqLength && (halfWidth / inSampleSize) > reqLength) {
        inSampleSize *= 2
      }
    }

    return inSampleSize
  }
}
