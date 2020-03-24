package de.ph1b.audiobook.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.WindowManager
import de.ph1b.audiobook.ffmpeg.ffmpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.math.min

// 500 kb
const val MAX_IMAGE_SIZE = 500 * 1024

@Singleton
class ImageHelper
@Inject
constructor(
  private val windowManager: Provider<WindowManager>,
  private val context: Context
) {

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
    val size = min(width, height)
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

  suspend fun getEmbeddedCover(f: File): Bitmap? = withContext(Dispatchers.IO) {
    val width = 1024
    val height = 1024
    val output = File(context.cacheDir, "cover_tmp.jpg")
    output.delete()
    ffmpeg(
      "-i",
      f.absolutePath,
      "-vf",
      "scale=iw*min($width/iw\\,$height/ih):ih*min($width/iw\\,$height/ih)," +
        "pad=$width:$height:($width-iw*min($width/iw\\,$height/ih))/2:($height-ih*min($width/iw\\,$height/ih))/2",
      output.absolutePath
    )
    if (output.exists()) {
      BitmapFactory.decodeFile(output.absolutePath)
    } else {
      null
    }
  }
}
