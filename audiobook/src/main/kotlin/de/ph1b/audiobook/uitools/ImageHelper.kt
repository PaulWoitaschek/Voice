package de.ph1b.audiobook.uitools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.view.WindowManager
import com.squareup.picasso.Picasso
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageHelper
@Inject
constructor(private val context: Context, private val windowManager: WindowManager, private val connectivityManager: ConnectivityManager) {

    fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun picassoGetBlocking(path: String): Bitmap? {
        val latch = CountDownLatch(1)
        val bitmap = arrayOfNulls<Bitmap>(1)
        Thread(Runnable {
            try {
                bitmap[0] = Picasso.with(context).load(path).get()
            } catch (e: IOException) {
                Timber.e(e, "Exception at file retrieving for %s", path)
            } finally {
                latch.countDown()
            }
        }).start()

        try {
            latch.await()
        } catch (e: InterruptedException) {
            Timber.wtf(e, "Latch was interrupted!")
        }

        return bitmap.first()
    }


    /**
     * Saves a bitmap as a file to the personal directory.

     * @param bitmap The bitmap to be saved
     */
    @Synchronized fun saveCover(bitmap: Bitmap, destination: File) {
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
            val coverOut = FileOutputStream(destination)
            try {
                bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 90, coverOut)
                coverOut.flush()
            } finally {
                coverOut.close()
            }
        } catch (e: IOException) {
            Timber.e(e, "Error at saving image with destination=%s", destination)
        }

    }

    val smallerScreenSize: Int
        @SuppressWarnings("deprecation")
        get() {
            val display = windowManager.defaultDisplay
            val displayWidth = display.width
            val displayHeight = display.height
            return if (displayWidth < displayHeight) displayWidth else displayHeight
        }

    val isOnline: Boolean
        get() = connectivityManager.activeNetworkInfo?.isConnected ?: false

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

        //setting reqWidth matching to desired 1:1 ratio and screen-size
        if (width < height) {
            reqLength *= (height / width)
        } else {
            reqLength *= (width / height)
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

