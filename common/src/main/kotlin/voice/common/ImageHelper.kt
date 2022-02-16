package voice.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.view.WindowManager
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.ffmpeg.ffmpeg
import voice.logging.core.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class ImageHelper
@Inject
constructor(
  private val windowManager: Provider<WindowManager>,
  private val context: Context
) {

  suspend fun saveCover(bitmap: Bitmap, destination: File) {
    withContext(Dispatchers.IO) {
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

      try {
        @Suppress("BlockingMethodInNonBlockingContext")
        FileOutputStream(destination).use {
          val compressFormat = when (destination.extension) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
              Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
              @Suppress("DEPRECATION")
              Bitmap.CompressFormat.WEBP
            }
            else -> error("Unhandled image extension for $destination")
          }
          bitmapToSave.compress(compressFormat, 70, it)
          it.flush()
        }
      } catch (e: IOException) {
        Logger.w(e, "Error at saving image with destination=$destination")
      }
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
      input = f.toUri(),
      context = context,
      command = listOf(
        "-vf",
        "scale=iw*min($width/iw\\,$height/ih):ih*min($width/iw\\,$height/ih)," +
          "pad=$width:$height:($width-iw*min($width/iw\\,$height/ih))/2:($height-ih*min($width/iw\\,$height/ih))/2",
        output.absolutePath
      )
    )
    if (output.exists()) {
      BitmapFactory.decodeFile(output.absolutePath)
    } else {
      null
    }
  }
}
